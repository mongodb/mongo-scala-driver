/*
 * Copyright 2016 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mongodb.scala.bson.codecs.macrocodecs

import scala.collection.MapLike
import scala.reflect.macros.whitebox

import org.bson.codecs.Codec
import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala.bson.annotations.Key

private[codecs] object CaseClassCodec {

  def createCodecDefaultCodecRegistryEncodeNone[T: c.WeakTypeTag](c: whitebox.Context)(): c.Expr[Codec[T]] = {
    import c.universe._
    createCodecDefaultCodecRegistry[T](c)(c.Expr[Boolean](q"true"))
  }

  def createCodecEncodeNone[T: c.WeakTypeTag](c: whitebox.Context)(codecRegistry: c.Expr[CodecRegistry]): c.Expr[Codec[T]] = {
    import c.universe._
    createCodec[T](c)(codecRegistry, c.Expr[Boolean](q"true"))
  }

  def createCodecDefaultCodecRegistryIgnoreNone[T: c.WeakTypeTag](c: whitebox.Context)(): c.Expr[Codec[T]] = {
    import c.universe._
    createCodecDefaultCodecRegistry[T](c)(c.Expr[Boolean](q"false"))
  }

  def createCodecIgnoreNone[T: c.WeakTypeTag](c: whitebox.Context)(codecRegistry: c.Expr[CodecRegistry]): c.Expr[Codec[T]] = {
    import c.universe._
    createCodec[T](c)(codecRegistry, c.Expr[Boolean](q"false"))
  }

  def createCodecDefaultCodecRegistry[T: c.WeakTypeTag](c: whitebox.Context)(encodeNone: c.Expr[Boolean]): c.Expr[Codec[T]] = {
    import c.universe._
    createCodec[T](c)(c.Expr[CodecRegistry](
      q"""
         import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
         DEFAULT_CODEC_REGISTRY
      """
    ), encodeNone)
  }

  // scalastyle:off method.length
  def createCodec[T: c.WeakTypeTag](c: whitebox.Context)(codecRegistry: c.Expr[CodecRegistry], encodeNone: c.Expr[Boolean]): c.Expr[Codec[T]] = {
    import c.universe._

    // Declared types
    val mainType = weakTypeOf[T]

    val stringType = typeOf[String]
    val mapTypeSymbol = typeOf[MapLike[_, _, _]].typeSymbol

    // Names
    val classTypeName = mainType.typeSymbol.name.toTypeName
    val codecName = TypeName(s"${classTypeName}MacroCodec")

    // Type checkers
    def isCaseClass(t: Type): Boolean = t.typeSymbol.isClass && t.typeSymbol.asClass.isCaseClass
    def isMap(t: Type): Boolean = t.baseClasses.contains(mapTypeSymbol)
    def isOption(t: Type): Boolean = t.typeSymbol == definitions.OptionClass
    def isTuple(t: Type): Boolean = definitions.TupleClass.seq.contains(t.typeSymbol)
    def isSealed(t: Type): Boolean = t.typeSymbol.isClass && t.typeSymbol.asClass.isSealed
    def isCaseClassOrSealed(t: Type): Boolean = isCaseClass(t) || isSealed(t)

    def allSubclasses(s: Symbol): Set[Symbol] = {
      val directSubClasses = s.asClass.knownDirectSubclasses
      directSubClasses ++ directSubClasses.flatMap({ s: Symbol => allSubclasses(s) })
    }
    val subClasses: List[Type] = allSubclasses(mainType.typeSymbol).map(_.asClass.toType).filter(isCaseClass).toList
    if (isSealed(mainType) && subClasses.isEmpty) c.abort(c.enclosingPosition, "No known subclasses of the sealed class")
    val knownTypes = (mainType +: subClasses).reverse

    val terms = mainType.decl(termNames.CONSTRUCTOR).asMethod.paramLists match {
      case h :: _ => h.map(_.asTerm)
      case _ => List.empty
    }

    val fields: Map[Type, List[(TermName, Type)]] = {
      knownTypes.map(
        t => (
          t,
          t.members.sorted.filter(_.isMethod).map(_.asMethod).filter(m => m.isGetter && m.isParamAccessor).map(
            m => (m.name, m.returnType.asSeenFrom(t, t.typeSymbol))
          )
        )
      ).toMap
    }

    val classAnnotatedFieldsMap: Map[TermName, Constant] = {
      terms.flatMap(t => {
        t.annotations.find(a => a.tree.tpe eq typeOf[Key])
          .flatMap(_.tree.children.lastOption)
          .map(tree => {
            t.name -> tree.productElement(0).asInstanceOf[Constant]
          })
      }).toMap
    }

    // Data converters
    def keyName(t: Type): Literal = Literal(Constant(t.typeSymbol.name.decodedName.toString))
    def keyNameTerm(t: TermName): Literal = Literal(classAnnotatedFieldsMap.getOrElse(t, Constant(t.toString)))

    // Primitives type map
    val primitiveTypesMap: Map[Type, Type] = Map(
      typeOf[Boolean] -> typeOf[java.lang.Boolean],
      typeOf[Byte] -> typeOf[java.lang.Byte],
      typeOf[Char] -> typeOf[java.lang.Character],
      typeOf[Double] -> typeOf[java.lang.Double],
      typeOf[Float] -> typeOf[java.lang.Float],
      typeOf[Int] -> typeOf[java.lang.Integer],
      typeOf[Long] -> typeOf[java.lang.Long],
      typeOf[Short] -> typeOf[java.lang.Short]
    )

    /*
     * For each case class sets the Map of the given field names and their field default value.
     */

    def defaultClassArgs = {
      val params = terms.zipWithIndex.collect {
        case (s, i) if s.isParamWithDefault => {
          val getterName = TermName("apply$default$" + (i + 1))
          q"${s.name.toString} -> ${mainType.typeSymbol.companion}.$getterName"
        }
      }
      c.Expr[Map[String, Any]](q"Map[String, Any](..$params)")
    }

    /*
     * Flattens the type args for any given type.
     *
     * Removes the key field from Maps as they have to be strings.
     * Removes Option type as the Option value is wrapped automatically below.
     * Throws if the case class contains a Tuple
     *
     * @param at the type to flatten the arguments for
     * @return a list of the type arguments for the type
     */
    def flattenTypeArgs(at: Type): List[c.universe.Type] = {
      val t = at.dealias
      val typeArgs = t.typeArgs match {
        case head :: _ if isMap(t) && head != stringType => c.abort(c.enclosingPosition, "Maps must contain string types for keys")
        case _ :: tail if isMap(t) /* head == stringType */ => tail
        case args => args
      }
      val types = t +: typeArgs.flatMap(x => flattenTypeArgs(x))
      if (types.exists(isTuple)) c.abort(c.enclosingPosition, "Tuples currently aren't supported in case classes")
      types.filterNot(isOption).map(x => primitiveTypesMap.getOrElse(x, x))
    }

    /*
     * Maps the given field names to type args for the values in the field
     *
     * ```
     *  addresses: Seq[Address] => (addresses, List[classOf[Seq], classOf[Address]])
     *  nestedAddresses: Seq[Seq[Address]] => (addresses, List[classOf[Seq], classOf[Seq], classOf[Address]])
     * ```
     *
     * @return a map of the field names with a list of the contain types
     */
    def createFieldTypeArgsMap(fields: List[(TermName, Type)]) = {
      val setTypeArgs = fields.map({
        case (name, f) =>
          val key = keyNameTerm(name)
          q"""
            typeArgs += ($key -> {
              val tpeArgs = mutable.ListBuffer.empty[Class[_]]
              ..${flattenTypeArgs(f).map(t => q"tpeArgs += classOf[${t.finalResultType}]")}
              tpeArgs.toList
            })"""
      })

      q"""
        val typeArgs = mutable.Map[String, List[Class[_]]]()
        ..$setTypeArgs
        typeArgs.toMap
      """
    }

    /*
     * For each case class sets the Map of the given field names and their field types.
     */
    def createClassFieldTypeArgsMap = {
      val setClassFieldTypeArgs = fields.map(field =>
        q"""
            classFieldTypeArgs += (${keyName(field._1)} -> ${createFieldTypeArgsMap(field._2)})
        """)

      q"""
        val classFieldTypeArgs = mutable.Map[String, Map[String, List[Class[_]]]]()
        ..$setClassFieldTypeArgs
        classFieldTypeArgs.toMap
      """
    }

    /*
     * Creates a `Map[String, Class[_]]` mapping the case class name and the type.
     *
     * @return the case classes map
     */
    def caseClassesMap = {
      val setSubClasses = knownTypes.map(t => q"caseClassesMap += (${keyName(t)} -> classOf[${t.finalResultType}])")
      q"""
        val caseClassesMap = mutable.Map[String, Class[_]]()
        ..$setSubClasses
        caseClassesMap.toMap
      """
    }

    /*
     * Creates a `Map[Class[_], Boolean]` mapping field types to a boolean representing if they are a case class.
     *
     * @return the class to case classes map
     */
    def classToCaseClassMap = {
      val flattenedFieldTypes = fields.flatMap({ case (t, types) => types.map(f => f._2) :+ t })
      val setClassToCaseClassMap = flattenedFieldTypes.map(t => q"""classToCaseClassMap ++= ${
        flattenTypeArgs(t).map(t =>
          q"(classOf[${t.finalResultType}], ${isCaseClassOrSealed(t)})")
      }""")

      q"""
        val classToCaseClassMap = mutable.Map[Class[_], Boolean]()
        ..$setClassToCaseClassMap
        classToCaseClassMap.toMap
      """
    }

    /*
     * Handles the writing of case class fields.
     *
     * @param fields the list of fields
     * @return the tree that writes the case class fields
     */
    def writeClassValues(fields: List[(TermName, Type)]): List[Tree] = {
      fields.map({
        case (name, f) =>
          val key = keyNameTerm(name)
          f match {
            case optional if isOption(optional) => q"""
              val localVal = instanceValue.$name
              if (localVal.isDefined) {
                writer.writeName($key)
                this.writeFieldValue($key, writer, localVal.get, encoderContext)
              } else if ($encodeNone) {
                writer.writeName($key)
                this.writeFieldValue($key, writer, this.bsonNull, encoderContext)
              }"""
            case _ => q"""
              val localVal = instanceValue.$name
              writer.writeName($key)
              this.writeFieldValue($key, writer, localVal, encoderContext)
              """
          }
      })
    }

    /*
     * Writes the Case Class fields and values to the BsonWriter
     */
    def writeValue: Tree = {
      val cases: Seq[Tree] = {
        fields.map(field => cq""" ${keyName(field._1)} =>
            val instanceValue = value.asInstanceOf[${field._1}]
            ..${writeClassValues(field._2)}""").toSeq
      }

      q"""
        writer.writeStartDocument()
        this.writeClassFieldName(writer, className, encoderContext)
        className match { case ..$cases }
        writer.writeEndDocument()
      """
    }

    def fieldSetters(fields: List[(TermName, Type)]) = {
      fields.map({
        case (name, f) =>
          val key = keyNameTerm(name)
          f match {
            case optional if isOption(optional) => q"$name = (if (fieldData.contains($key)) Option(fieldData($key)) else None).asInstanceOf[$f]"
            case _ => q"$name = fieldData.getOrElse($key, classFieldDefaultArgsMap($key)).asInstanceOf[$f]"
          }
      })
    }

    def getInstance = {
      val cases = knownTypes.map { st =>
        cq"${keyName(st)} => new $st(..${fieldSetters(fields(st))})"
      } :+ cq"""_ => throw new CodecConfigurationException("Unexpected class type: " + className)"""
      q"className match { case ..$cases }"
    }

    c.Expr[Codec[T]](
      q"""
        import scala.collection.mutable
        import org.bson.BsonWriter
        import org.bson.codecs.EncoderContext
        import org.bson.codecs.configuration.{CodecRegistry, CodecConfigurationException}
        import org.mongodb.scala.bson.codecs.macrocodecs.MacroCodec

        case class $codecName(codecRegistry: CodecRegistry) extends MacroCodec[$classTypeName] {
          val classFieldDefaultArgsMap = $defaultClassArgs
          val caseClassesMap = $caseClassesMap
          val classToCaseClassMap = $classToCaseClassMap
          val classFieldTypeArgsMap = $createClassFieldTypeArgsMap
          val encoderClass = classOf[$classTypeName]
          def getInstance(className: String, fieldData: Map[String, Any]) = $getInstance
          def writeCaseClassData(className: String, writer: BsonWriter, value: $mainType, encoderContext: EncoderContext) = $writeValue
        }

      ${codecName.toTermName}($codecRegistry).asInstanceOf[Codec[$mainType]]
      """
    )
  }
  // scalastyle:on method.length
}
