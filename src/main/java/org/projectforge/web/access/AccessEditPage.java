/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2010 Kai Reinhard (k.reinhard@me.com)
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

package org.projectforge.web.access;

import org.apache.log4j.Logger;
import org.apache.wicket.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.access.AccessDao;
import org.projectforge.access.GroupTaskAccessDO;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.EditPage;

@EditPage(defaultReturnPage = AccessListPage.class)
public class AccessEditPage extends AbstractEditPage<GroupTaskAccessDO, AccessEditForm, AccessDao> implements ISelectCallerPage
{
  private static final long serialVersionUID = 4636922408954211544L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AccessEditPage.class);

  @SpringBean(name = "accessDao")
  private AccessDao accessDao;

  public AccessEditPage(final PageParameters parameters)
  {
    super(parameters, "access");
    super.init();
  }

  public void cancelSelection(String property)
  {
    // Do nothing.
  }

  public void select(String property, Object selectedValue)
  {
    if ("taskId".equals(property) == true) {
      accessDao.setTask(getData(), (Integer) selectedValue);
    } else if ("groupId".equals(property) == true) {
      accessDao.setGroup(getData(), (Integer) selectedValue);
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }

  public void unselect(String property)
  {
    if ("taskId".equals(property) == true) {
      getData().setTask(null);
    } else if ("groupId".equals(property) == true) {
      getData().setGroup(null);
    } else {
      log.error("Property '" + property + "' not supported for unselection.");
    }
  }

  @Override
  protected AccessDao getBaseDao()
  {
    return accessDao;
  }

  @Override
  protected AccessEditForm newEditForm(AbstractEditPage< ? , ? , ? > parentPage, GroupTaskAccessDO data)
  {
    return new AccessEditForm(this, data);
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
