package com.neowit.apexscanner.nodes.soql

import com.neowit.apexscanner.ast.QualifiedName
import com.neowit.apexscanner.nodes.{AstNode, AstNodeType, FromNodeType, IsTypeDefinition, Range, ValueType, ValueTypeSimple}

/**
  * Created by Andrey Gavrikov 
  */
case class FromNode(qualifiedName: Option[QualifiedName], aliasOpt: Option[String], range: Range) extends AstNode with IsTypeDefinition {
    override def nodeType: AstNodeType = FromNodeType

    override def getValueType: Option[ValueType] = qualifiedName.map(ValueTypeSimple)

    override protected def resolveDefinitionImpl(): Option[AstNode] = {
        qualifiedName match {
            case Some(_) => Option(this)
            case None => None
        }
    }
}