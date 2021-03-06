/*
 * Copyright (c) 2017 Andrey Gavrikov.
 *
 * This file is part of https://github.com/neowit/apexscanner
 *
 *  This file is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This file is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package com.neowit.apexscanner.nodes.soql

import com.neowit.apexscanner.ast.QualifiedName
import com.neowit.apexscanner.nodes.{AstNode, AstNodeType, FromNodeType, IsTypeDefinition, Range, SoqlQueryNodeType, ValueType, ValueTypeSimple}

/**
  * Created by Andrey Gavrikov 
  */

object SoqlQueryNode {
    val LIBRARY_NAME: String = "SObjectLibrary"
    val CHILD_RELATIONSHIPS_NODE_NAME: String = "_Child_Relationships"
}
/**
  *
  * @param queryStr text version of SOQL query
  * @param range location range
  */
case class SoqlQueryNode(queryStr: String, range: Range) extends AstNode with IsTypeDefinition {

    override def nodeType: AstNodeType = SoqlQueryNodeType

    override def isScope: Boolean = true

    override def getValueType: Option[ValueType] = {
        getFromNodes match {
            case nodes if nodes.isEmpty =>
                //return Database Namespace which can return all DB Objects
                Option(ValueTypeSimple(QualifiedName(SoqlQueryNode.LIBRARY_NAME)))
            case nodes if 1 == nodes.length =>
                nodes.head.getValueType // return FromNode as is
            case nodes =>
                // multiple object types defined in From clause
                // can not return singular value type
                None

        }
    }

    override val qualifiedName: Option[QualifiedName] = Option(new QualifiedName(Array(queryStr)))

    override protected def resolveDefinitionImpl(actionContext: com.neowit.apexscanner.scanner.actions.ActionContext): Option[AstNode] = Option(this)

    def getAliases: Seq[String] = {
        getFromNodes match {
            case nodes if nodes.isEmpty =>
                Seq.empty
            case nodes =>
                // multiple object types defined in From clause
                nodes.map(_.aliasOpt.getOrElse("")).filterNot(_.isEmpty)

        }
    }
    private def getFromNodes: Seq[FromNode] = {
        findChildrenInAst(_.nodeType == FromNodeType).map(_.asInstanceOf[FromNode])
    }


    override def toString: String = "QUERY: " + queryStr
}
