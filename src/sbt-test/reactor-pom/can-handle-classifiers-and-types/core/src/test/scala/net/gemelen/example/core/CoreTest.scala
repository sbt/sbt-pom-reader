package net.gemelen.example.core

import net.gemelen.example.annotation.ExampleAnnotation1
import net.gemelen.example.annotation.ExampleAnnotation2

class CoreTest {

    @ExampleAnnotation1
    @ExampleAnnotation2(version = "something")
    def f(): Unit = {}
}
