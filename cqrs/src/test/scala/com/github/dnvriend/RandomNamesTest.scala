package com.github.dnvriend

class RandomNamesTest extends TestSpec {
  it should "get names from classpath" in {
    randomNames() should not be 'empty
  }

  it should "get name from a list" in {
    nextName(List("a")) shouldBe "a"
    nextName(List("a", "b")) shouldBe a[String]
  }
}
