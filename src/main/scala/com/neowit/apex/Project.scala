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

package com.neowit.apex

import java.nio.file.{FileSystems, Path}

import com.neowit.apex.stdlib.StandardLibrary
import com.neowit.apex.stdlib.impl.StdLibLocal

/**
  * Created by Andrey Gavrikov 
  */
case class Project(path: Path) {
    private var _stdLib: Option[StandardLibrary] = None

    def getStandardLibrary: StandardLibrary = {
        _stdLib match {
          case Some(lib) => lib
          case None =>
              val lib = loadStandardLibrary()
              _stdLib = Option(lib)
             lib
        }
    }

    private def loadStandardLibrary(): StandardLibrary = {

        // TODO implement proper resolution of path to STD Library
        val stdLibDir = "TODO"
        val path = FileSystems.getDefault.getPath(stdLibDir)
        new StdLibLocal(path)
    }
}
