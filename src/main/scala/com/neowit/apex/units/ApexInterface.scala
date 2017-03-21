package com.neowit.apex.units

case class ApexInterface(annotationOpt: Option[ApexAnnotation],
                         modifiers: Modifiers,
                         name: String,
                         superTypeOpt: Option[ClassLike],
                         implements: List[ClassLike],
                         parentContext: Option[ApexUnit]) extends ClassLike
