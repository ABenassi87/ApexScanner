package com.neowit.apex.nodes

case class MethodParameter(name: String, locationInterval: LocationInterval) extends AstNode {
    override def nodeType: AstNodeType = MethodParameterNodeType
}