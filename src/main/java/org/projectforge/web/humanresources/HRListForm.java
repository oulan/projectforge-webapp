/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2013 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.web.humanresources;

import java.util.Date;

import org.apache.log4j.Logger;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.projectforge.humanresources.HRFilter;
import org.projectforge.web.calendar.QuickSelectWeekPanel;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.components.DatePanelSettings;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.HtmlCommentPanel;

/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class HRListForm extends AbstractListForm<HRFilter, HRListPage>
{
  private static final long serialVersionUID = -5511800187080680095L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(HRListForm.class);

  protected DatePanel startDate;

  protected DatePanel stopDate;

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    final HRFilter filter = getSearchFilter();
    super.init();
    gridBuilder.newGridPanel();
    final FieldsetPanel fs = gridBuilder.newFieldset(getString("timePeriod"), true);
    startDate = new DatePanel(fs.newChildId(), new PropertyModel<Date>(filter, "startTime"), DatePanelSettings.get()
        .withSelectPeriodMode(true).withRequired(true));
    fs.add(startDate);
    fs.add(new DivTextPanel(fs.newChildId(), " - "));
    stopDate = new DatePanel(fs.newChildId(), new PropertyModel<Date>(filter, "stopTime"), DatePanelSettings.get()
        .withSelectPeriodMode(true).withRequired(true));
    fs.add(stopDate);
    final QuickSelectWeekPanel quickSelectPanel = new QuickSelectWeekPanel(fs.newChildId(), new Model<Date>() {
      @Override
      public Date getObject()
      {
        startDate.validate(); // Update model from form field.
        final Date date = startDate.getConvertedInput();
        return date;
      }
    }, parentPage, "week");
    fs.add(quickSelectPanel);
    quickSelectPanel.init();
    fs.add(new DivTextPanel(fs.newChildId(), new Model<String>() {
      @Override
      public String getObject()
      {
        return WicketUtils.getCalendarWeeks(HRListForm.this, filter.getStartTime(), filter.getStopTime());
      }
    }));
    fs.add(new HtmlCommentPanel(fs.newChildId(), new Model<String>() {
      @Override
      public String getObject()
      {
        return WicketUtils.getUTCDates(filter.getStartTime(), filter.getStopTime());
      }
    }));
  }

  public HRListForm(final HRListPage parentPage)
  {
    super(parentPage);
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#onOptionsPanelCreate(org.projectforge.web.wicket.flowlayout.FieldsetPanel,
   *      org.projectforge.web.wicket.flowlayout.DivPanel)
   */
  @Override
  protected void onOptionsPanelCreate(final FieldsetPanel optionsFieldsetPanel, final DivPanel optionsCheckBoxesPanel)
  {
    optionsCheckBoxesPanel.add(createAutoRefreshCheckBoxPanel(optionsCheckBoxesPanel.newChildId(), new PropertyModel<Boolean>(getSearchFilter(),
        "showPlanning"), getString("hr.planning.filter.showPlanning")));
    optionsCheckBoxesPanel.add(createAutoRefreshCheckBoxPanel(optionsCheckBoxesPanel.newChildId(), new PropertyModel<Boolean>(getSearchFilter(),
        "showBookedTimesheets"), getString("hr.planning.filter.showBookedTimesheets")));
    optionsCheckBoxesPanel.add(createAutoRefreshCheckBoxPanel(optionsCheckBoxesPanel.newChildId(), new PropertyModel<Boolean>(getSearchFilter(),
        "onlyMyProjects"), getString("hr.planning.filter.onlyMyProjects")));
    optionsCheckBoxesPanel.add(createAutoRefreshCheckBoxPanel(optionsCheckBoxesPanel.newChildId(), new PropertyModel<Boolean>(getSearchFilter(),
        "allProjectsGroupedByCustomer"), getString("hr.planning.filter.allProjectsGroupedByCustomer")));
    optionsCheckBoxesPanel.add(createAutoRefreshCheckBoxPanel(optionsCheckBoxesPanel.newChildId(), new PropertyModel<Boolean>(getSearchFilter(),
        "otherProjectsGroupedByCustomer"), getString("hr.planning.filter.otherProjectsGroupedByCustomer")));
  }

  @Override
  protected boolean isSearchFilterVisible()
  {
    return false;
  }

  @Override
  protected HRFilter newSearchFilterInstance()
  {
    return new HRFilter();
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#showOptionsPanel()
   */
  @Override
  protected boolean showOptionsPanel()
  {
    return true;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#showHistorySearchAndDeleteCheckbox()
   */
  @Override
  protected boolean showHistorySearchAndDeleteCheckbox()
  {
    return true;
  }
}
