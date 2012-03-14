/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2012 Kai Reinhard (k.reinhard@micromata.com)
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

package org.projectforge.web.mobile;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.core.BaseDO;
import org.projectforge.web.wicket.AbstractEditPage;
import org.springframework.util.CollectionUtils;

public abstract class AbstractMobileListPage<F extends AbstractMobileListForm< ? , ? >, D extends org.projectforge.core.IDao< ? >, O extends BaseDO< ? >>
extends AbstractSecuredMobilePage
{
  private static final long serialVersionUID = 4104862404249463041L;

  protected static final int MAX_ROWS = 50;

  protected F form;

  protected List<O> list;

  protected String i18nKey;

  protected WebMarkupContainer resultList;

  protected ListViewPanel listViewPanel;

  public AbstractMobileListPage(final String i18nKey, final PageParameters parameters)
  {
    super(parameters);
    this.i18nKey = i18nKey;
    form = newListForm(this);
    add(form);
    form.init();
    setNoBackButton();
  }

  @SuppressWarnings("unchecked")
  protected void search()
  {
    if (listViewPanel != null) {
      remove(listViewPanel);
    }
    list = (List<O>) getBaseDao().getList(form.filter);
    add(listViewPanel = new ListViewPanel("listViewPage"));
    if (CollectionUtils.isEmpty(list) == true) {
      listViewPanel.setVisible(false);
      return;
    }

    int counter = 0;
    for (final O entry : list) {
      final PageParameters params = new PageParameters();
      params.add(AbstractEditPage.PARAMETER_KEY_ID, entry.getId());
      final String comment = getEntryComment(entry);
      final ListViewItemPanel listItem = new ListViewItemPanel(listViewPanel.newChildId(), getEditPageClass(), params,
          getEntryName(entry));
      if (StringUtils.isNotBlank(comment) == true) {
        listItem.setComment(", " + comment);
      }
      listViewPanel.add(listItem);
      if (++counter >= MAX_ROWS) {
        break;
      }
    }
    if (list.size() > MAX_ROWS) {
      listViewPanel.addMoreEntriesAvailable();
    }
  }

  @Override
  protected void onBeforeRender()
  {
    super.onBeforeRender();
    search();
  }

  /**
   * @return The value to show in the list.
   */
  protected abstract String getEntryName(final O entry);

  /**
   * @return The value to show as a comment in the list or null at default.
   */
  protected String getEntryComment(final O entry)
  {
    return null;
  }

  protected abstract D getBaseDao();

  @Override
  protected void addTopRightButton()
  {
    final PageParameters params = new PageParameters();
    params.add(AbstractMobileEditPage.PARAMETER_KEY_EDIT, true);
    headerContainer.add(new JQueryButtonPanel(TOP_RIGHT_BUTTON_ID, JQueryButtonType.PLUS, getEditPageClass(), params, getString("new"))
    .setRelDialog());
  }

  protected abstract Class<? extends WebPage> getEditPageClass();

  protected abstract F newListForm(AbstractMobileListPage< ? , ? , ? > parentPage);

  @Override
  protected String getTitle()
  {
    return getString(i18nKey + ".title.heading");
  }
}
