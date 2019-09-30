/*
 * Copyright (c) 2019, NVIDIA CORPORATION.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.rapids.spark

import org.apache.spark.SparkConf

class JoinsSuite extends SparkQueryCompareTestSuite {

  INCOMPAT_testSparkResultsAreEqual2("Test broadcast hash join", longsDf, nonZeroLongsDf,
    conf=new SparkConf()
      .set("spark.sql.autoBroadcastJoinThreshold", "10MB")) {
    (A, B) => A.join(B, A("longs") === B("more_longs"))
  }

  INCOMPAT_testSparkResultsAreEqual2("Test broadcast hash join with ops", longsDf, nonZeroLongsDf,
    conf=new SparkConf()
      .set("spark.sql.autoBroadcastJoinThreshold", "10MB")) {
    (A, B) => A.join(B, (A("longs") - A("more_longs")) === (B("longs") - B("more_longs")))
  }

  INCOMPAT_testSparkResultsAreEqual2("Test broadcast hash join with conditional", longsDf, nonZeroLongsDf,
    conf=new SparkConf()
      .set("spark.sql.autoBroadcastJoinThreshold", "10MB")) {
    (A, B) => A.join(B, A("longs") === B("longs") && A("more_longs") >= B("more_longs"))
  }

  INCOMPAT_IGNORE_ORDER_testSparkResultsAreEqual2("Test broadcast hash join with mixed fields",
    mixedDf, mixedDfWithNulls,
    conf = new SparkConf()
      .set("spark.sql.autoBroadcastJoinThreshold", "10MB")) {
    (A, B) => A.join(B, A("ints") === B("ints"))
  }

  INCOMPAT_IGNORE_ORDER_testSparkResultsAreEqual2("Test broadcast hash join on string with mixed fields",
    mixedDf, mixedDfWithNulls,
    conf = new SparkConf()
      .set("spark.sql.autoBroadcastJoinThreshold", "10MB")) {
    (A, B) => A.join(B, A("strings") === B("strings"))
  }

  // For spark to insert a shuffled hash join it has to be enabled with
  // "spark.sql.join.preferSortMergeJoin" = "false" and both sides have to
  // be larger than a broadcast hash join would want
  // "spark.sql.autoBroadcastJoinThreshold", but one side has to be smaller
  // than the number of splits * broadcast threshold and also be at least
  // 3 times smaller than the other side.  So it is not likely to happen
  // unless we can give it some help.
  INCOMPAT_IGNORE_ORDER_testSparkResultsAreEqual2("Test hash join", longsDf, biggerLongsDf,
    conf = new SparkConf()
      .set("spark.sql.autoBroadcastJoinThreshold", "160")
      .set("spark.sql.join.preferSortMergeJoin", "false")
      .set("spark.sql.shuffle.partitions", "2")) { // hack to try and work around bug in cudf
    (A, B) => A.join(B, A("longs") === B("more_longs"))
  }

  // test replacement of sort merge join with hash join
  // make sure broadcast size small enough it doesn't get used
  testSparkResultsAreEqual2("Test replace sort merge join with hash join",
    longsDf, biggerLongsDf,
    conf = new SparkConf()
      .set("spark.sql.autoBroadcastJoinThreshold", "-1")
      .set("spark.sql.join.preferSortMergeJoin", "true")
      .set("spark.sql.shuffle.partitions", "2"), // hack to try and work around bug in cudf
    incompat = true,
    allowNonGpu = false,
    sort = true,
    execsAllowedNonGpu = Seq("SortExec", "SortOrder")) {
    (A, B) => A.join(B, A("longs") === B("longs"))
  }
}