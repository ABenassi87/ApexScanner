package com.neowit.apex.scanner

import com.neowit.apex.scanner.actions.SyntaxError
import org.antlr.v4.runtime.ANTLRErrorListener

/**
  * Created by Andrey Gavrikov on 20/03/2017.
  */
trait ApexErrorListener extends ANTLRErrorListener{
    /**
      * @return list of syntax error accumulated while scanning source(s)
      */
    def result(): Seq[SyntaxError]
}