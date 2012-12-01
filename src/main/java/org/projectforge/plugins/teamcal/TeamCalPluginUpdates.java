/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2012 Kai Reinhard (k.reinhard@micromata.de)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.plugins.teamcal;

import org.projectforge.admin.UpdateEntry;
import org.projectforge.admin.UpdateEntryImpl;
import org.projectforge.admin.UpdatePreCheckStatus;
import org.projectforge.admin.UpdateRunningStatus;
import org.projectforge.database.DatabaseUpdateDao;
import org.projectforge.database.Table;
import org.projectforge.plugins.teamcal.admin.TeamCalDO;
import org.projectforge.plugins.teamcal.event.TeamEventDO;

/**
 * Contains the initial data-base set-up script and later all update scripts if any data-base schema updates are required by any later
 * release of this to-do plugin.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class TeamCalPluginUpdates
{
  static DatabaseUpdateDao dao;

  @SuppressWarnings("serial")
  public static UpdateEntry getInitializationUpdateEntry()
  {
    return new UpdateEntryImpl(TeamCalPlugin.ID, "1.0.0", "2012-05-09", "Adds tables T_PLUGIN_CALENDAR_*.") {

      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        final Table calendarTable = new Table(TeamCalDO.class);
        final Table eventTable = new Table(TeamEventDO.class);
        // Does the data-base table already exist?
        return dao.doesExist(calendarTable) == true && dao.doesExist(eventTable) == true ? UpdatePreCheckStatus.ALREADY_UPDATED
            : UpdatePreCheckStatus.OK;
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        final Table calendarTable = new Table(TeamCalDO.class);
        final Table eventTable = new Table(TeamEventDO.class);
        //         Create initial data-base table:
        if (dao.doesExist(calendarTable) == false) {
          calendarTable.addDefaultBaseDOAttributes().addAttributes("owner", "fullAccessGroup", "readOnlyAccessGroup", "minimalAccessGroup",
              "description", "title");
          dao.createTable(calendarTable);
        }
        if (dao.doesExist(eventTable) == false) {
          eventTable.addDefaultBaseDOAttributes().addAttributes("subject", "location", "calendar", "startDate", "endDate",
              "note", "attendees", "recurrenceInterval", "recurrenceAmount", "recurrenceEndDate");
          dao.createTable(eventTable);
        }
        dao.createMissingIndices();
        return UpdateRunningStatus.DONE;
      }
    };
  }
}
