package net.gemelen.example.annotation

import scala.annotation.StaticAnnotation
import scala.annotation.meta._

@param @field @getter @setter @beanGetter @beanSetter
class ExampleAnnotation2(version: String) extends StaticAnnotation
