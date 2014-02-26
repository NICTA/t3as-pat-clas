/*
    Copyright 2013 NICTA
    
    This file is part of t3as (Text Analysis As A Service).

    t3as is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    t3as is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with t3as.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.t3as.patClas.lookup.factory

import scala.slick.driver.ExtendedProfile

import org.apache.commons.dbcp.BasicDataSource
import org.t3as.patClas.common.Util, Util.{getProperty => get}

import javax.sql.DataSource

object LookupProperties {
  implicit val p = Util.properties("/lookup.properties")
  
  private[factory] val dataSource: DataSource = {    
    val ds = new BasicDataSource
    ds.setDriverClassName(get("jdbc.driverClassName"))
    ds.setUsername(get("jdbc.username"))
    ds.setPassword(get("jdbc.password"))
    ds.setMaxActive(get("jdbc.maxActive").toInt);
    ds.setMaxIdle(get("jdbc.maxIdle").toInt);
    ds.setInitialSize(get("jdbc.initialSize").toInt);
    ds.setValidationQuery(get("jdbc.validationQuery"))
    ds.setUrl(get("jdbc.url"))

    // test the data source validity
    ds.getConnection().close()
    ds
  }
  
  private[factory] val slickDriver: ExtendedProfile = Util.getObject(get("slick.driverObjectName"))
}
