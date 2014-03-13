/**
 * Copyright 2010-2014 MongoDB, Inc. <http://www.mongodb.org>
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * For questions and comments about this product, please see the project page at:
 *
 * https://github.com/mongodb/mongo-scala-driver
 *
 */
package org.mongodb.scala.unit


import org.mongodb.{ReadPreference, WriteConcern}
import scala.collection.JavaConverters._
import org.mongodb.connection.Tags
import org.mongodb.ReadPreference.secondaryPreferred

import org.mongodb.scala.MongoClientURI
import org.scalatest.FlatSpec
import java.util.logging.Logger
import org.mongodb.diagnostics.Loggers
import org.mongodb.scala.helpers.UnitTestSpec


class MongoClientUriSpec extends UnitTestSpec {

  "MongoClientURI" should "throw Exception if URI does not have a trailing slash" in {
    a[IllegalArgumentException] should be thrownBy {
      MongoClientURI("mongodb://localhost?wTimeout=5")
    }
  }

  it should "parse uri strings into correct components" in {

    val password = Some("pass".toCharArray)
    val uriExamples =
      Table(
        ("uri"                                               , "hosts"                               , "database"  , "collection"   , "username"  , "password"),
        ("mongodb://db.example.com"                          , List("db.example.com")                , None        , None           , None        , None      ),
        ("mongodb://10.0.0.1"                                , List("10.0.0.1")                      , None        , None           , None        , None      ),
        ("mongodb://[::1]"                                   , List("[::1]")                         , None        , None           , None        , None      ),
        ("mongodb://foo/bar"                                 , List("foo")                           , Some("bar") , None           , None        , None      ),
        ("mongodb://10.0.0.1/bar"                            , List("10.0.0.1")                      , Some("bar") , None           , None        , None      ),
        ("mongodb://[::1]/bar"                               , List("[::1]")                         , Some("bar") , None           , None        , None      ),
        ("mongodb://localhost/test.my.coll"                  , List("localhost")                     , Some("test"), Some("my.coll"), None        , None      ),
        ("mongodb://foo/bar.goo"                             , List("foo")                           , Some("bar") , Some("goo")    , None        , None      ),
        ("mongodb://user:pass@host/bar"                      , List("host")                          , Some("bar") , None           , Some("user"), password  ),
        ("mongodb://user:pass@host:27011/bar"                , List("host:27011")                    , Some("bar") , None           , Some("user"), password  ),
        ("mongodb://user:pass@10.0.0.1:27011/bar"            , List("10.0.0.1:27011")                , Some("bar") , None           , Some("user"), password  ),
        ("mongodb://user:pass@[::1]:27011/bar"               , List("[::1]:27011")                   , Some("bar") , None           , Some("user"), password  ),
        ("mongodb://user:pass@host:7,host2:8,host3:9/bar"    , List("host:7", "host2:8", "host3:9")  , Some("bar") , None           , Some("user"), password  ),
        ("mongodb://user:pass@10.0.0.1:7,[::1]:8,host3:9/bar", List("10.0.0.1:7","[::1]:8","host3:9"), Some("bar") , None           , Some("user"), password  )
      )

    forAll(uriExamples) {
      (uri: String, hosts: List[String], database: Option[String], collection: Option[String], username: Option[String],
       password: Option[Array[Char]]) =>
        val mongoClientURI = MongoClientURI(uri)

        mongoClientURI.hosts should equal(hosts)
        mongoClientURI.database should equal(database)
        mongoClientURI.collection should equal(collection)

        val credentials = mongoClientURI.mongoCredentials
        credentials match {
          case Some(credential) => {
            credential.getUserName should equal(username.get)
            password match {
              case Some(pass) => credential.getPassword should equal(pass)
              case None => credential.getPassword should equal(null)
            }
          }
          case None =>
            username should be(None)
            password should be(None)
        }
    }
  }

  it should "have correct defaults for options" in {
    val options = MongoClientURI("mongodb://localhost").options
    options.description should be("")
    options.readPreference should be(ReadPreference.primary)
    options.writeConcern should be(WriteConcern.ACKNOWLEDGED)
    options.minConnectionPoolSize should be(0)
    options.maxConnectionPoolSize should be(100)
    options.threadsAllowedToBlockForConnectionMultiplier should be(5)
    options.maxWaitTime should be(1000 * 60 * 2)
    options.maxConnectionIdleTime should be(0)
    options.maxConnectionLifeTime should be(0)
    options.connectTimeout should be(1000 * 10)
    options.socketTimeout should be(0)
    options.socketKeepAlive should be(false)
    options.autoConnectRetry should be(false)
    options.maxAutoConnectRetryTime should be(0)
    options.SSLEnabled should be(false)
    options.heartbeatFrequency should be(5000)
    options.heartbeatConnectRetryFrequency should be(10)
    options.heartbeatConnectTimeout should be(20000)
    options.heartbeatSocketTimeout should be(20000)
    options.requiredReplicaSetName should be(None)
  }

  it should "return the correct write concern" in {
    val writeConcernExamples = Table(
      ("uri"                                                          , "writeConcern"                             ),
      ("mongodb://localhost"                                          , WriteConcern.ACKNOWLEDGED                  ),
      ("mongodb://localhost/?safe=true"                               , WriteConcern.ACKNOWLEDGED                  ),
      ("mongodb://localhost/?safe=false"                              , WriteConcern.UNACKNOWLEDGED                ),
      ("mongodb://localhost/?wTimeout=5"                              , new WriteConcern(1, 5, false, false)       ),
      ("mongodb://localhost/?fsync=true"                              , new WriteConcern(1, 0, true, false)        ),
      ("mongodb://localhost/?j=true"                                  , new WriteConcern(1, 0, false, true)        ),
      ("mongodb://localhost/?w=2&wtimeout=5&fsync=true&j=true"        , new WriteConcern(2, 5, true, true)         ),
      ("mongodb://localhost/?w=majority&wtimeout=5&fsync=true&j=true" , new WriteConcern("majority", 5, true, true))
    )

    forAll(writeConcernExamples) {
      (uri: String, writeConcern: WriteConcern) =>
        val mongoClientURI = MongoClientURI(uri)
        mongoClientURI.options.writeConcern should equal(writeConcern)
    }
  }

  it should "return the correct read preference" in {
    val readPreferenceExamples = Table(
      ("uri"                                                       , "readPreference                                                 "),
      ("mongodb://localhost/?readPreference=secondaryPreferred"    , ReadPreference.secondaryPreferred                                ),
      ("""mongodb://localhost/?readPreference=secondaryPreferred
          &readPreferenceTags=dc:ny,rack:1&readPreferenceTags=dc:ny
          &readPreferenceTags=""".replaceAll("\n[ ]+", "")          ,  secondaryPreferred(List(new Tags("dc", "ny").append("rack", "1"),
                                                                                              new Tags("dc", "ny"), new Tags()).asJava))
    )
    forAll(readPreferenceExamples) {
      (uri: String, readPreference: ReadPreference) =>
        val mongoClientURI = MongoClientURI(uri)
        mongoClientURI.options.readPreference should equal(readPreference)
    }
  }

  it should "be able to handle & or ; or a mix of separators" in {
    val uriExamples = Table(
      """mongodb://localhost/?minPoolSize=5&maxPoolSize=10&waitQueueMultiple=5&
             waitQueueTimeoutMS=150&maxIdleTimeMS=200&maxLifeTimeMS=300&
             replicaSet=test&connectTimeoutMS=2500&socketTimeoutMS=5500&
             autoConnectRetry=true&slaveOk=true&safe=false&w=1&
             wtimeout=2500&fsync=true""".replaceAll("\n[ ]+", ""),
      """mongodb://localhost/?minPoolSize=5;maxPoolSize=10;waitQueueMultiple=5;
             waitQueueTimeoutMS=150;maxIdleTimeMS=200;maxLifeTimeMS=300;
             replicaSet=test;connectTimeoutMS=2500;socketTimeoutMS=5500;
             autoConnectRetry=true;slaveOk=true;safe=false;w=1;
             wtimeout=2500;fsync=true""".replaceAll("\n[ ]+", ""),
      """mongodb://localhost/test?minPoolSize=5;maxPoolSize=10&waitQueueMultiple=5;
             waitQueueTimeoutMS=150;maxIdleTimeMS=200&maxLifeTimeMS=300&
             replicaSet=test;connectTimeoutMS=2500;socketTimeoutMS=5500&
             autoConnectRetry=true;slaveOk=true;safe=false&w=1;
             wtimeout=2500;fsync=true""".replaceAll("\n[ ]+", ""))

    forAll(uriExamples) {
      (uri: String) =>
        val options = MongoClientURI(uri).options
        options.minConnectionPoolSize should be(5)
        options.maxConnectionPoolSize should be(10)
        options.threadsAllowedToBlockForConnectionMultiplier should be(5)
        options.maxWaitTime should be(150)
        options.maxConnectionIdleTime should be(200)
        options.maxConnectionLifeTime should be(300)
        options.socketTimeout should be(5500)
        options.autoConnectRetry should be(true)
        options.writeConcern should equal(new WriteConcern(1, 2500, true))
        options.readPreference should equal(ReadPreference.secondaryPreferred())
        options.requiredReplicaSetName should be(Some("test"))
    }
  }

  it should "log any unsupported keys" in withLogCapture("uri") {
    customHandler =>

      val uri = "mongodb://localhost/?minPoolSize=1&madeUp=2&socketThymeout=1&w=1"
      MongoClientURI(uri)

      val expectedMessages: Set[String] = Set(
        s"WARNING: Unsupported option 'socketthymeout' on URI '$uri'.",
        s"WARNING: Unsupported option 'madeup' on URI '$uri'."
      )
      getLogMessages should equal(expectedMessages)
  }
}
