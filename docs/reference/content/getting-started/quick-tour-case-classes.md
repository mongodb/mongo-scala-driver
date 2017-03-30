+++
date = "2015-03-17T15:36:56Z"
title = "Quick Tour - case classes"
[menu.main]
  parent = "Getting Started"
  identifier = "Quick Tour - case classes"
  weight = 35
  pre = "<i class='fa'></i>"
+++

# Quick Tour with case classes

The following code snippets come from the `QuickTourCaseClass.scala` example code that can be found with the 
[driver source]({{< srcref "examples/src/test/scala/tour/QuickTourCaseClass.scala">}}).

{{% note class="important" %}}
This follows on from the [quick tour]({{< relref "getting-started/quick-tour.md" >}}).

See the [Bson macros]({{< relref "bson/macros.md" >}}) documentation for in-depth information about using macros for configuring case class
support with your `MongoCollection`.
{{% /note %}}


First we'll create the case class we want to use to represent the documents in the collection. In the following we create a `Person` case
class and companion object:

```scala
import org.mongodb.scala.bson.ObjectId
object Person {
  def apply(firstName: String, lastName: String): Person =
    Person(new ObjectId(), firstName, lastName)
}
case class Person(_id: ObjectId, firstName: String, lastName: String)
```

{{% note %}}
You'll notice in the companion object we automatically assign a `_id` when creating new instances that don't include it. The `_id` is the 
primary key for a document, so by having a `_id` field in the case class it allows access to the primary key. 
{{% /note %}}

## Configuring case classes

Then when using `Person` with our collection we must have a `Codec` that can convert it to and from `BSON`. The `bson` package provides 
macros that automatically generate a codec at compile time. In the following example we create a new `CodecRegistry` that includes a 
codec for the `Person` case class:


```scala
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.bson.codecs.configuration.CodecRegistries.{fromRegistries, fromProviders}

val codecRegistry = fromRegistries(fromProviders(classOf[Person]), DEFAULT_CODEC_REGISTRY )
```

Once we have the `codecRegistry` configured then we can use that to create a `MongoCollection[Person]`.  In the following we get the `test` collection on the `mydb` database. Notice the `codecRegistry` is set at the database level, it could have been set when creating the `MongoClient` or even via the `MongoCollection.withCodecRegistry` method.

```scala
// To directly connect to the default server localhost on port 27017
val mongoClient: MongoClient = MongoClient()
val database: MongoDatabase = mongoClient.getDatabase("mydb").withCodecRegistry(codecRegistry)
val collection: MongoCollection[Person] = database.getCollection("test")
```

## Insert a person

Once you have the collection object, you can insert `Person` instances into the collection:

```scala
val person: Person = Person("Ada", "Lovelace")
collection.insertOne(person).results()
```

## Add multiple instances

To add multiple `Person` instances, you can use the `insertMany()` method and print the results of the operation:

```scala
val people: Seq[Person] = Seq(
  Person("Charles", "Babbage"),
  Person("George", "Boole"),
  Person("Gertrude", "Blanch"),
  Person("Grace", "Hopper"),
  Person("Ida", "Rhodes"),
  Person("Jean", "Bartik"),
  Person("John", "Backus"),
  Person("Lucy", "Sanders"),
  Person("Tim", "Berners Lee"),
  Person("Zaphod", "Beeblebrox")
)
collection.insertMany(people).printResults()
```
And we should the following output:

```
The operation completed successfully
```

## Querying the collection

Use the [find()]({{< apiref "org.mongodb.scala.MongoCollection@find[C](filter:org.bson.conversions.Bson)(implicite:org.mongodb.scala.Helpers.DefaultsTo[C,org.mongodb.scala.collection.immutable.Document],implicitct:scala.reflect.ClassTag[C]):org.mongodb.scala.FindObservable[C]">}})
method to query the collection.

### Find the First Person in a Collection

You can query the collection in the same as shown in the [quick tour]({{< relref "getting-started/quick-tour.md" >}}).

```scala
collection.find().first().printHeadResult()
```

The example will print the first `Person` in the database:

```
Person(58dd0a68218de22333435fa4, Ada, Lovelace)
```

### Find all people in the collection

To retrieve all the people in the collection, we will use the
`find()` method. The `find()` method returns a `FindObservable` instance that
provides a fluent interface for chaining or controlling find operations. 
The following code retrieves all documents in the collection and prints them out
(101 documents). Using the `printResults()` implicit we block until the observer is completed and then print each result:
```scala
collection.find().printResults()
```


## Get a single person with a query filter

We can create a filter to pass to the find() method to get a subset of
the documents in our collection. For example, if we wanted to find the
document for which the value of the "firstName" is "Ida", we would do the
following:

```scala
import org.mongodb.scala.model.Filters._

collection.find(equal("firstName", "Ida")).first().printHeadResult()
```

will eventually print just one Person:

```
Person(58dd0a68218de22333435fa4, Ida, Rhodes)
```

{{% note %}}
Use the [`Filters`]({{< relref "builders/filters.md">}}), [`Sorts`]({{< relref "builders/sorts.md">}}),
[`Projections`]({{< relref "builders/projections.md">}}) and [`Updates`]({{< relref "builders/updates.md">}})
helpers for simple and concise ways of building up queries.
{{% /note %}}

## Get a set of people with a query

We can use the query to get a set of people from our collection. For
example, if we wanted to get all documents where the `firstName` starts with `G` and sort by `lastName` we could
write:

```scala
collection.find(regex("firstName", "^G")).sort(ascending("lastName")).printResults()
```

Which will print out the Person instances for Gertrude, George and Grace.

## Updating documents

There are numerous [update operators](http://docs.mongodb.org/manual/reference/operator/update-field/)
supported by MongoDB.  We can use the [Updates]({{< apiref "org.mongodb.scala.model.Updates$">}}) helpers to help update documents in the database.

In the following we update and correct the hyphenation of Tim Berners-Lee: 

```scala
collection.updateOne(equal("lastName", "Berners Lee"), set("lastName", "Berners-Lee")).printHeadResult("Update Result: ")
```

The update methods return an [`UpdateResult`]({{< coreapiref "com/mongodb/client/result/UpdateResult.html">}}),
which provides information about the operation including the number of documents modified by the update.

## Deleting documents

To delete at most a single document (may be 0 if none match the filter) use the [`deleteOne`]({{< apiref "org.mongodb.scala.MongoCollection@deleteOne(filter:org.bson.conversions.Bson):org.mongodb.scala.Observable[org.mongodb.scala.result.DeleteResult]">}})
method:

```scala
collection.deleteOne(equal("firstName", "Zaphod")).printHeadResult("Delete Result: ")
```

As you can see the API allows for easy use of CRUD operations with case classes. See the [Bson macros]({{< relref "bson/macros.md" >}}) 
documentation for further information about the macros.
