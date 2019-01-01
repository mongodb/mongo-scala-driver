package org.mongodb.scala.model

import java.lang.reflect.Modifier._

import org.bson.{BsonDocument, BsonType}
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.geojson.{Point, Polygon, Position}
import org.mongodb.scala.model.Filters.FiltersDsl._
import org.mongodb.scala.{MongoClient, model}
import org.scalatest.{FlatSpec, Matchers}

//scalastyle:off null
class FiltersDslSpec extends FlatSpec with Matchers {

  private def toBson(bson: Bson): Document =
    Document(bson.toBsonDocument(classOf[BsonDocument], MongoClient.DEFAULT_CODEC_REGISTRY))

  "Filters" should "have the same methods as the wrapped Filters" in {
    val wrapped = classOf[com.mongodb.client.model.Filters].getDeclaredMethods
      .filter(f => isStatic(f.getModifiers) && isPublic(f.getModifiers)).map(_.getName).toSet
    val aliases = Set("equal", "notEqual", "bsonType")
    val ignore = Set("$anonfun$geoWithinPolygon$1")
    val local = model.Filters.getClass.getDeclaredMethods
      .filter(f => isPublic(f.getModifiers))
      .map(_.getName)
      .toSet -- aliases -- ignore

    local should equal(wrapped)
  }

  it should "render without $eq" in {
    toBson("x" $eq 1) should equal(Document("""{x : 1}"""))
    toBson("x" $eq null) should equal(Document("""{x : null}"""))

    toBson("x" $equal 1) should equal(Document("""{x : 1}"""))
    toBson("x" $equal null) should equal(Document("""{x : null}"""))
  }

  it should "render $ne" in {
    toBson("x" $ne 1) should equal(Document("""{x : {$ne : 1} }"""))
    toBson("x" $ne null) should equal(Document("""{x : {$ne : null} }"""))
  }

  it should "render $not" in {
    toBson($not("x" $exists false)) should equal(Document("""{x : {$not: {$exists: false}}}"""))
    toBson("x" $not { _ $exists false }) should equal(Document("""{x : {$not: {$exists: false}}}"""))

    toBson($not("x" $eq 1)) should equal(Document("""{x : {$not: {$eq: 1}}}"""))
    toBson("x" $not { _ $eq 1 }) should equal(Document("""{x : {$not: {$eq: 1}}}"""))

    toBson($not("x" $gt 1)) should equal(Document("""{x : {$not: {$gt: 1}}}"""))
    toBson("x" $not { _ $gt 1 }) should equal(Document("""{x : {$not: {$gt: 1}}}"""))

    toBson($not("x" $regex "^p.*")) should equal(Document("""{x : {$not: /^p.*/}}"""))
    toBson("x" $not { _ $regex "^p.*" }) should equal(Document("""{x : {$not: /^p.*/}}"""))

    toBson($not($and("x" $gt 1, "y" $eq 20))) should equal(Document("""{$not: {$and: [{x: {$gt: 1}}, {y: 20}]}}"""))
    toBson($not($and("x" $eq 1, "x" $eq 2))) should equal(Document("""{$not: {$and: [{x: 1}, {x: 2}]}}"""))
    toBson($not($and("x" $in (1, 2), "x" $eq 3))) should equal(Document("""{$not: {$and: [{x: {$in: [1, 2]}}, {x: 3}]}}"""))
    toBson($not($or("x" $gt 1, "y" $eq 20))) should equal(Document("""{$not: {$or: [{x: {$gt: 1}}, {y: 20}]}}"""))
    toBson($not($or("x" $eq 1, "x" $eq 2))) should equal(Document("""{$not: {$or: [{x: 1}, {x: 2}]}}"""))
    toBson($not($or("x" $in (1, 2), "x" $eq 3))) should equal(Document("""{$not: {$or: [{x: {$in: [1, 2]}}, {x: 3}]}}"""))
    toBson($not(Document("$in" -> List(1)))) should equal(Document("""{$not: {$in: [1]}}"""))
  }

  it should "render $nor" in {
    toBson($nor("price" $eq 1)) should equal(Document("""{$nor : [{price: 1}]}"""))
    toBson($nor("price" $eq 1, "sale" $eq true)) should equal(Document("""{$nor : [{price: 1}, {sale: true}]}"""))
  }

  it should "render $gt" in {
    toBson("x" $gt 1) should equal(Document("""{x : {$gt : 1} }"""))
  }

  it should "render $lt" in {
    toBson("x" $lt 1) should equal(Document("""{x : {$lt : 1} }"""))
  }

  it should "render $gte" in {
    toBson("x" $gte 1) should equal(Document("""{x : {$gte : 1} }"""))
  }

  it should "render $lte" in {
    toBson("x" $lte 1) should equal(Document("""{x : {$lte : 1} }"""))
  }

  it should "render $exists" in {
    toBson("x".$exists) should equal(Document("""{x : {$exists : true} }"""))
    toBson("x" $exists true) should equal(Document("""{x : {$exists : true} }"""))
    toBson("x" $exists false) should equal(Document("""{x : {$exists : false} }"""))
  }

  it should "or should render empty or using $or" in {
    toBson($or()) should equal(Document("""{$or : []}"""))
  }

  it should "render $or" in {
    toBson($or("x" $eq 1, "y" $eq 2)) should equal(Document("""{$or : [{x : 1}, {y : 2}]}"""))
  }

  it should "and should render empty and using $and" in {
    toBson($and()) should equal(Document("""{$and : []}"""))
  }

  it should "and should render and without using $and" in {
    toBson($and("x" $eq 1, "y" $eq 2)) should equal(Document("""{x : 1, y : 2}"""))
  }

  it should "and should render $and with clashing keys" in {
    toBson($and("a" $eq 1, "a" $eq 2)) should equal(Document("""{$and: [{a: 1}, {a: 2}]}"""))
  }

  it should "and should flatten multiple operators for the same key" in {
    toBson($and("a" $gt 1, "a" $lt 9)) should equal(Document("""{a : {$gt : 1, $lt : 9}}"""))
  }

  it should "and should flatten nested" in {
    toBson($and($and("a" $eq 1, "b" $eq 2), "c" $eq 3)) should equal(Document("""{a : 1, b : 2, c : 3}"""))
    toBson($and($and("a" $eq 1, "a" $eq 2), "c" $eq 3)) should equal(Document("""{$and:[{a : 1}, {a : 2}, {c : 3}] }"""))
    toBson($and("a" $lt 1, "b" $lt 2)) should equal(Document("""{a : {$lt : 1}, b : {$lt : 2} }"""))
    toBson($and("a" $lt 1, "a" $lt 2)) should equal(Document("""{$and : [{a : {$lt : 1}}, {a : {$lt : 2}}]}"""))
  }

  it should "render $all" in {
    toBson("a" $all (1, 2, 3)) should equal(Document("""{a : {$all : [1, 2, 3]} }"""))
  }

  it should "render $elemMatch" in {
    toBson("results" $elemMatch Document("$gte" -> 80, "$lt" -> 85)) should equal(
      Document("""{results : {$elemMatch : {$gte: 80, $lt: 85}}}""")
    )
    toBson("results" $elemMatch $and("product" $eq "xyz", "score" $gt 8)) should equal(
      Document("""{ results : {$elemMatch : {product : "xyz", score : {$gt : 8}}}}""")
    )
  }

  it should "render $in" in {
    toBson("a" $in (1, 2, 3)) should equal(Document("""{a : {$in : [1, 2, 3]} }"""))
  }

  it should "render $nin" in {
    toBson("a" $nin (1, 2, 3)) should equal(Document("""{a : {$nin : [1, 2, 3]} }"""))
  }

  it should "render $mod" in {
    toBson("a" $mod (divisor = 100, remainder = 7)) should equal(Document("a" -> Document("$mod" -> List(100L, 7L))))
  }

  it should "render $size" in {
    toBson("a" $size 13) should equal(Document("""{a : {$size : 13} }"""))
  }

  it should "render $type" in {
    toBson("a" $type BsonType.ARRAY) should equal(Document("""{a : {$type : 4} }"""))
  }

  it should "render $bitsAllClear" in {
    toBson("a" $bitsAllClear 13) should equal(Document("""{a : {$bitsAllClear : { "$numberLong" : "13" }} }"""))
  }

  it should "render $bitsAllSet" in {
    toBson("a" $bitsAllSet 13) should equal(Document("""{a : {$bitsAllSet : { "$numberLong" : "13" }} }"""))
  }

  it should "render $bitsAnyClear" in {
    toBson("a" $bitsAnyClear 13) should equal(Document("""{a : {$bitsAnyClear : { "$numberLong" : "13" }} }"""))
  }

  it should "render $bitsAnySet" in {
    toBson("a" $bitsAnySet 13) should equal(Document("""{a : {$bitsAnySet : { "$numberLong" : "13" }} }"""))
  }

  it should "render $text" in {
    toBson($text("mongoDB for GIANT ideas")) should equal(
      Document("""{$text: {$search: "mongoDB for GIANT ideas"} }""")
    )
    toBson($text("mongoDB for GIANT ideas", "english")) should equal(
      Document("""{$text: {$search: "mongoDB for GIANT ideas", $language : "english"}}""")
    )
    toBson($text("mongoDB for GIANT ideas", new TextSearchOptions().language("english"))) should equal(
      Document("""{$text : {$search : "mongoDB for GIANT ideas", $language : "english"} }""")
    )
    toBson($text("mongoDB for GIANT ideas", new TextSearchOptions().caseSensitive(true))) should equal(
      Document("""{$text : {$search : "mongoDB for GIANT ideas", $caseSensitive : true} }""")
    )
    toBson($text("mongoDB for GIANT ideas", new TextSearchOptions().diacriticSensitive(false))) should equal(
      Document("""{$text : {$search : "mongoDB for GIANT ideas", $diacriticSensitive : false} }""")
    )
    toBson($text("mongoDB for GIANT ideas", new TextSearchOptions().language("english").caseSensitive(false)
      .diacriticSensitive(true))) should equal(
      Document(
        """{$text : {$search : "mongoDB for GIANT ideas", $language : "english", $caseSensitive : false,
              $diacriticSensitive : true} }"""
      )
    )
  }

  it should "render $regex" in {
    toBson("name" $regex "acme.*corp") should equal(Document("""{name : {$regex : "acme.*corp", $options : ""}}"""))
    toBson("name" $regex ("acme.*corp", "si")) should equal(Document("""{name : {$regex : "acme.*corp", $options : "si"}}"""))
    toBson("name" $regex "acme.*corp".r) should equal(Document("""{name : {$regex : "acme.*corp", $options : ""}}"""))
  }

  it should "render $where" in {
    toBson($where("this.credits == this.debits")) should equal(Document("""{$where: "this.credits == this.debits"}"""))
  }

  it should "render $geoWithin" in {
    val polygon = Polygon(Seq(
      Position(40.0, 18.0),
      Position(40.0, 19.0),
      Position(41.0, 19.0),
      Position(40.0, 18.0)
    ))

    toBson("loc" $geoWithin polygon) should equal(Document("""{
                                                          loc: {
                                                            $geoWithin: {
                                                              $geometry: {
                                                                type: "Polygon",
                                                                coordinates: [
                                                                               [
                                                                                 [40.0, 18.0], [40.0, 19.0], [41.0, 19.0], [40.0, 18.0]
                                                                               ]
                                                                             ]
                                                              }
                                                            }
                                                          }
                                                        }"""))

    toBson("loc" $geoWithin Document(polygon.toJson)) should equal(Document("""{
                                                                          loc: {
                                                                            $geoWithin: {
                                                                              $geometry: {
                                                                                type: "Polygon",
                                                                                coordinates: [
                                                                                               [
                                                                                                 [40.0, 18.0], [40.0, 19.0], [41.0, 19.0],
                                                                                                 [40.0, 18.0]
                                                                                               ]
                                                                                             ]
                                                                              }
                                                                            }
                                                                          }
                                                                        }"""))
  }

  it should "render $geoWithin with $box" in {
    toBson("loc" $geoWithinBox (1d, 2d, 3d, 4d)) should equal(Document("""{
                                                                    loc: {
                                                                      $geoWithin: {
                                                                        $box:  [
                                                                                 [ 1.0, 2.0 ], [ 3.0, 4.0 ]
                                                                               ]
                                                                      }
                                                                    }
                                                                  }"""))
  }

  it should "render $geoWithin with $polygon" in {
    toBson("loc" $geoWithinPolygon List(List(0d, 0d), List(3d, 6d), List(6d, 0d))) should equal(Document("""{
                                                                                        loc: {
                                                                                          $geoWithin: {
                                                                                            $polygon: [
                                                                                                        [ 0.0, 0.0 ], [ 3.0, 6.0 ],
                                                                                                        [ 6.0, 0.0 ]
                                                                                                      ]
                                                                                          }
                                                                                        }
                                                                                      }"""))
  }

  it should "render $geoWithin with $center" in {
    toBson("loc" $geoWithinCenter (-74d, 40.74d, 10d)) should equal(Document("""{ loc: { $geoWithin: { $center: [ [-74.0, 40.74], 10.0 ] } } }"""))
  }

  it should "render $geoWithin with $centerSphere" in {
    toBson("loc" $geoWithinCenterSphere (-74d, 40.74d, 10d)) should equal(Document("""{
                                                                                 loc: {
                                                                                   $geoWithin: {
                                                                                     $centerSphere: [
                                                                                                      [-74.0, 40.74], 10.0
                                                                                                    ]
                                                                                   }
                                                                                 }
                                                                              }"""))
  }

  it should "render $geoIntersects" in {
    val polygon = Polygon(Seq(
      Position(40.0d, 18.0d),
      Position(40.0d, 19.0d),
      Position(41.0d, 19.0d),
      Position(40.0d, 18.0d)
    ))

    toBson("loc" $geoIntersects polygon) should equal(Document("""{
                                                               loc: {
                                                                 $geoIntersects: {
                                                                   $geometry: {
                                                                      type: "Polygon",
                                                                      coordinates: [
                                                                                     [
                                                                                       [40.0, 18.0], [40.0, 19.0], [41.0, 19.0],
                                                                                       [40.0, 18.0]
                                                                                     ]
                                                                                   ]
                                                                   }
                                                                 }
                                                               }
                                                            }"""))

    toBson("loc" $geoIntersects Document(polygon.toJson)) should equal(Document("""{
                                                                              loc: {
                                                                                $geoIntersects: {
                                                                                  $geometry: {
                                                                                    type: "Polygon",
                                                                                    coordinates: [
                                                                                                   [
                                                                                                     [40.0, 18.0], [40.0, 19.0], [41.0, 19.0],
                                                                                                     [40.0, 18.0]
                                                                                                   ]
                                                                                                 ]
                                                                                  }
                                                                                }
                                                                              }
                                                                            }"""))
  }

  it should "render $near" in {
    val point = Point(Position(-73.9667, 40.78))
    val pointDocument = Document(point.toJson)

    toBson("loc" $near point) should equal(Document("""{
                                                                   loc : {
                                                                      $near: {
                                                                         $geometry: {
                                                                            type : "Point",
                                                                            coordinates : [ -73.9667, 40.78 ]
                                                                         },
                                                                      }
                                                                   }
                                                                 }"""))

    toBson("loc" $near (point, Some(5000d), Some(1000d))) should equal(Document("""{
                                                                         loc : {
                                                                            $near: {
                                                                               $geometry: {
                                                                                  type : "Point",
                                                                                  coordinates : [ -73.9667, 40.78 ]
                                                                               },
                                                                               $maxDistance: 5000.0,
                                                                               $minDistance: 1000.0,
                                                                            }
                                                                         }
                                                                       }"""))

    toBson("loc" $near (point, Some(5000d), None)) should equal(Document("""{
                                                                        loc : {
                                                                           $near: {
                                                                              $geometry: {
                                                                                 type : "Point",
                                                                                 coordinates : [ -73.9667, 40.78 ]
                                                                              },
                                                                              $maxDistance: 5000.0,
                                                                           }
                                                                        }
                                                                      }"""))

    toBson("loc" $near (point, None, Some(1000d))) should equal(Document("""{
                                                                        loc : {
                                                                           $near: {
                                                                              $geometry: {
                                                                                 type : "Point",
                                                                                 coordinates : [ -73.9667, 40.78 ]
                                                                              },
                                                                              $minDistance: 1000.0,
                                                                           }
                                                                        }
                                                                      }"""))

    toBson("loc" $near (point, None, None)) should equal(Document("""{
                                                                        loc : {
                                                                           $near: {
                                                                              $geometry: {
                                                                                 type : "Point",
                                                                                 coordinates : [ -73.9667, 40.78 ]
                                                                              },
                                                                           }
                                                                        }
                                                                      }"""))

    toBson("loc" $near pointDocument) should equal(Document("""{
                                                                           loc : {
                                                                              $near: {
                                                                                 $geometry: {
                                                                                    type : "Point",
                                                                                    coordinates : [ -73.9667, 40.78 ]
                                                                                 },
                                                                              }
                                                                           }
                                                                         }"""))

    toBson("loc" $near (pointDocument, Some(5000d), Some(1000d))) should equal(Document("""{
                                                                                 loc : {
                                                                                    $near: {
                                                                                       $geometry: {
                                                                                          type : "Point",
                                                                                          coordinates : [ -73.9667, 40.78 ]
                                                                                       },
                                                                                       $maxDistance: 5000.0,
                                                                                       $minDistance: 1000.0,
                                                                                    }
                                                                                 }
                                                                               }"""))

    toBson("loc" $near (pointDocument, Some(5000d), None)) should equal(Document("""{
                                                                                loc : {
                                                                                   $near: {
                                                                                      $geometry: {
                                                                                         type : "Point",
                                                                                         coordinates : [ -73.9667, 40.78 ]
                                                                                      },
                                                                                      $maxDistance: 5000.0,
                                                                                   }
                                                                                }
                                                                              }"""))

    toBson("loc" $near (pointDocument, None, Some(1000d))) should equal(Document("""{
                                                                                loc : {
                                                                                   $near: {
                                                                                      $geometry: {
                                                                                         type : "Point",
                                                                                         coordinates : [ -73.9667, 40.78 ]
                                                                                      },
                                                                                      $minDistance: 1000.0,
                                                                                   }
                                                                                }
                                                                              }"""))

    toBson("loc" $near (pointDocument, None, None)) should equal(Document("""{
                                                                                loc : {
                                                                                   $near: {
                                                                                      $geometry: {
                                                                                         type : "Point",
                                                                                         coordinates : [ -73.9667, 40.78 ]
                                                                                      },
                                                                                   }
                                                                                }
                                                                              }"""))

    toBson("loc" $near (-73.9667, 40.78)) should equal(Document("""{
                                                                             loc : {
                                                                                $near: [-73.9667, 40.78],
                                                                                }
                                                                             }
                                                                           }"""))

    toBson("loc" $near (-73.9667, 40.78, Some(5000d), Some(1000d))) should equal(Document("""{
                                                                                   loc : {
                                                                                      $near: [-73.9667, 40.78],
                                                                                      $maxDistance: 5000.0,
                                                                                      $minDistance: 1000.0,
                                                                                      }
                                                                                   }
                                                                                 }"""))

    toBson("loc" $near (-73.9667, 40.78, Some(5000d), None)) should equal(Document("""{
                                                                                  loc : {
                                                                                     $near: [-73.9667, 40.78],
                                                                                     $maxDistance: 5000.0,
                                                                                     }
                                                                                  }
                                                                                }"""))

    toBson("loc" $near (-73.9667, 40.78, None, Some(1000d))) should equal(Document("""{
                                                                                  loc : {
                                                                                     $near: [-73.9667, 40.78],
                                                                                     $minDistance: 1000.0,
                                                                                     }
                                                                                  }
                                                                                }"""))

    toBson("loc" $near (-73.9667, 40.78, None, None)) should equal(Document("""{
                                                                                  loc : {
                                                                                     $near: [-73.9667, 40.78],
                                                                                     }
                                                                                  }
                                                                                }"""))
  }

  it should "render $nearSphere" in {
    val point = Point(Position(-73.9667, 40.78))
    val pointDocument = Document(point.toJson)

    toBson("loc" $nearSphere point) should equal(Document("""{
                                                                         loc : {
                                                                            $nearSphere: {
                                                                               $geometry: {
                                                                                  type : "Point",
                                                                                  coordinates : [ -73.9667, 40.78 ]
                                                                               },
                                                                            }
                                                                         }
                                                                       }"""))

    toBson("loc" $nearSphere (point, Some(5000d), Some(1000d))) should equal(Document("""{
                                                                               loc : {
                                                                                  $nearSphere: {
                                                                                     $geometry: {
                                                                                        type : "Point",
                                                                                        coordinates : [ -73.9667, 40.78 ]
                                                                                     },
                                                                                     $maxDistance: 5000.0,
                                                                                     $minDistance: 1000.0,
                                                                                  }
                                                                               }
                                                                             }"""))

    toBson("loc" $nearSphere (point, Some(5000d), None)) should equal(Document("""{
                                                                             loc:
                                                                             {
                                                                                 $nearSphere:
                                                                                 {
                                                                                     $geometry:
                                                                                     {
                                                                                         type: "Point",
                                                                                         coordinates:
                                                                                         [-73.9667, 40.78]
                                                                                     },
                                                                                     $maxDistance: 5000.0,
                                                                                 }
                                                                             }
                                                                         }"""))

    toBson("loc" $nearSphere (point, None, Some(1000d))) should equal(Document("""{
                                                                              loc : {
                                                                                 $nearSphere: {
                                                                                    $geometry: {
                                                                                       type : "Point",
                                                                                       coordinates : [ -73.9667, 40.78 ]
                                                                                    },
                                                                                    $minDistance: 1000.0,
                                                                                 }
                                                                              }
                                                                            }"""))

    toBson("loc" $nearSphere (point, None, None)) should equal(Document("""{
                                                                              loc : {
                                                                                 $nearSphere: {
                                                                                    $geometry: {
                                                                                       type : "Point",
                                                                                       coordinates : [ -73.9667, 40.78 ]
                                                                                    },
                                                                                 }
                                                                              }
                                                                            }"""))

    toBson("loc" $nearSphere pointDocument) should equal(Document("""{
                                                                                 loc : {
                                                                                    $nearSphere: {
                                                                                       $geometry: {
                                                                                          type : "Point",
                                                                                          coordinates : [ -73.9667, 40.78 ]
                                                                                       },
                                                                                    }
                                                                                 }
                                                                               }"""))

    toBson("loc" $nearSphere (pointDocument, Some(5000d), Some(1000d))) should equal(Document("""{
                                                                                       loc : {
                                                                                          $nearSphere: {
                                                                                             $geometry: {
                                                                                                type : "Point",
                                                                                                coordinates : [ -73.9667, 40.78 ]
                                                                                             },
                                                                                             $maxDistance: 5000.0,
                                                                                             $minDistance: 1000.0,
                                                                                          }
                                                                                       }
                                                                                     }"""))

    toBson("loc" $nearSphere (pointDocument, Some(5000d), None)) should equal(Document("""{
                                                                                      loc : {
                                                                                         $nearSphere: {
                                                                                            $geometry: {
                                                                                               type : "Point",
                                                                                               coordinates : [ -73.9667, 40.78 ]
                                                                                            },
                                                                                            $maxDistance: 5000.0,
                                                                                         }
                                                                                      }
                                                                                    }"""))

    toBson("loc" $nearSphere (pointDocument, None, Some(1000d))) should equal(Document("""{
                                                                                      loc : {
                                                                                         $nearSphere: {
                                                                                            $geometry: {
                                                                                               type : "Point",
                                                                                               coordinates : [ -73.9667, 40.78 ]
                                                                                            },
                                                                                            $minDistance: 1000.0,
                                                                                         }
                                                                                      }
                                                                                    }"""))

    toBson("loc" $nearSphere (pointDocument, None, None)) should equal(Document("""{
                                                                                      loc : {
                                                                                         $nearSphere: {
                                                                                            $geometry: {
                                                                                               type : "Point",
                                                                                               coordinates : [ -73.9667, 40.78 ]
                                                                                            },
                                                                                         }
                                                                                      }
                                                                                    }"""))

    toBson("loc" $nearSphere (-73.9667, 40.78)) should equal(Document("""{
                                                                                   loc : {
                                                                                      $nearSphere: [-73.9667, 40.78],
                                                                                      }
                                                                                   }
                                                                                 }"""))

    toBson("loc" $nearSphere (-73.9667, 40.78, Some(5000d), Some(1000d))) should equal(Document("""{
                                                                                         loc : {
                                                                                            $nearSphere: [-73.9667, 40.78],
                                                                                            $maxDistance: 5000.0,
                                                                                            $minDistance: 1000.0,
                                                                                            }
                                                                                         }
                                                                                       }"""))

    toBson("loc" $nearSphere (-73.9667, 40.78, Some(5000d), None)) should equal(Document("""{
                                                                                        loc : {
                                                                                           $nearSphere: [-73.9667, 40.78],
                                                                                           $maxDistance: 5000.0,
                                                                                           }
                                                                                        }
                                                                                      }"""))

    toBson("loc" $nearSphere (-73.9667, 40.78, None, Some(1000d))) should equal(Document("""{
                                                                                        loc : {
                                                                                           $nearSphere: [-73.9667, 40.78],
                                                                                           $minDistance: 1000.0,
                                                                                           }
                                                                                        }
                                                                                      }"""))

    toBson("loc" $nearSphere (-73.9667, 40.78, None, None)) should equal(Document("""{
                                                                                        loc : {
                                                                                           $nearSphere: [-73.9667, 40.78],
                                                                                           }
                                                                                        }
                                                                                      }"""))
  }

  it should "render $expr" in {
    toBson($expr(Document("{$gt: ['$spent', '$budget']}"))) should equal(Document("""{$expr: {$gt: ["$spent", "$budget"]}}"""))
  }

  it should "render $jsonSchema" in {
    toBson($jsonSchema(Document("{bsonType: 'object'}"))) should equal(Document("""{$jsonSchema: {bsonType:  "object"}}"""))
  }

}
//scalastyle:on null
