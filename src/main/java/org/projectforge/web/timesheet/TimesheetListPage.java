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

package org.projectforge.web.timesheet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.hibernate.Hibernate;
import org.projectforge.common.DateFormatType;
import org.projectforge.common.DateFormats;
import org.projectforge.common.DateHelper;
import org.projectforge.common.DateHolder;
import org.projectforge.common.FileHelper;
import org.projectforge.common.MyBeanComparator;
import org.projectforge.core.SystemInfoCache;
import org.projectforge.jira.JiraUtils;
import org.projectforge.renderer.PdfRenderer;
import org.projectforge.renderer.custom.Formatter;
import org.projectforge.renderer.custom.FormatterFactory;
import org.projectforge.task.TaskDO;
import org.projectforge.task.TaskTree;
import org.projectforge.timesheet.TimesheetDO;
import org.projectforge.timesheet.TimesheetDao;
import org.projectforge.timesheet.TimesheetExport;
import org.projectforge.timesheet.TimesheetFilter;
import org.projectforge.user.PFUserDO;
import org.projectforge.user.UserGroupCache;
import org.projectforge.user.UserPrefArea;
import org.projectforge.web.HtmlHelper;
import org.projectforge.web.calendar.DateTimeFormatter;
import org.projectforge.web.task.TaskFormatter;
import org.projectforge.web.task.TaskPropertyColumn;
import org.projectforge.web.user.UserFormatter;
import org.projectforge.web.user.UserPrefListPage;
import org.projectforge.web.user.UserPropertyColumn;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.DownloadUtils;
import org.projectforge.web.wicket.IListPageColumnsCreator;
import org.projectforge.web.wicket.ListPage;
import org.projectforge.web.wicket.ListSelectActionPanel;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;
import org.projectforge.web.wicket.flowlayout.CheckBoxPanel;

@ListPage(editPage = TimesheetEditPage.class)
public class TimesheetListPage extends AbstractListPage<TimesheetListForm, TimesheetDao, TimesheetDO> implements
IListPageColumnsCreator<TimesheetDO>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TimesheetListPage.class);

  protected static final String[] BOOKMARKABLE_PROPERTIES = mergeStringArrays(BOOKMARKABLE_FILTER_PROPERTIES, new String[] { "userId|user",
      "taskId|task", "startTime|t1", "stopTime|t2", "marked", "longFormat|long", "recursive"});

  /**
   * Key for pre-setting the task id.
   */
  public static final String PARAMETER_KEY_TASK_ID = "taskId";

  public static final String PARAMETER_KEY_SEARCHSTRING = "searchString";

  public static final String PARAMETER_KEY_USER_ID = "userId";

  public static final String PARAMETER_KEY_START_TIME = "startTime";

  public static final String PARAMETER_KEY_STOP_TIME = "stopTime";

  public static final String PARAMETER_KEY_CLEAR_ALL = "clear";

  private static final long serialVersionUID = 8582874051700734977L;

  @SpringBean(name = "dateTimeFormatter")
  private DateTimeFormatter dateTimeFormatter;

  @SpringBean(name = "formatterFactory")
  private FormatterFactory formatterFactory;

  @SpringBean(name = "pdfRenderer")
  private PdfRenderer pdfRenderer;

  @SpringBean(name = "timesheetDao")
  private TimesheetDao timesheetDao;

  @SpringBean(name = "timesheetExport")
  private TimesheetExport timesheetExport;

  @SpringBean(name = "taskFormatter")
  private TaskFormatter taskFormatter;

  @SpringBean(name = "taskTree")
  private TaskTree taskTree;

  @SpringBean(name = "userFormatter")
  private UserFormatter userFormatter;

  @SpringBean(name = "userGroupCache")
  private UserGroupCache userGroupCache;

  public TimesheetListPage(final PageParameters parameters)
  {
    super(parameters, "timesheet");
    if (WicketUtils.contains(parameters, PARAMETER_KEY_CLEAR_ALL) == true) {
      final boolean clear = WicketUtils.getAsBoolean(parameters, PARAMETER_KEY_CLEAR_ALL);
      if (clear == true) {
        form.getSearchFilter().setTaskId(null);
        form.getSearchFilter().setSearchString(null);
        form.getSearchFilter().setUserId(null);
        form.getSearchFilter().setStartTime(null);
        form.getSearchFilter().setStopTime(null);
        form.getSearchFilter().setRecursive(true);
        form.getSearchFilter().setMarked(false);
        form.getSearchFilter().setDeleted(false);
      }
    }
    if (WicketUtils.contains(parameters, PARAMETER_KEY_TASK_ID) == true) {
      final Integer id = WicketUtils.getAsInteger(parameters, PARAMETER_KEY_TASK_ID);
      form.getSearchFilter().setTaskId(id);
    }
    if (WicketUtils.contains(parameters, PARAMETER_KEY_SEARCHSTRING) == true) {
      final String searchString = WicketUtils.getAsString(parameters, PARAMETER_KEY_SEARCHSTRING);
      form.getSearchFilter().setSearchString(searchString);
    }
    if (WicketUtils.contains(parameters, PARAMETER_KEY_USER_ID) == true) {
      final Integer id = WicketUtils.getAsInteger(parameters, PARAMETER_KEY_USER_ID);
      form.getSearchFilter().setUserId(id);
    }
    if (WicketUtils.contains(parameters, PARAMETER_KEY_START_TIME) == true) {
      final Long time = WicketUtils.getAsLong(parameters, PARAMETER_KEY_START_TIME);
      if (time != null) {
        form.getSearchFilter().setStartTime(new Date(time));
      } else {
        form.getSearchFilter().setStartTime(null);
      }
    }
    if (WicketUtils.contains(parameters, PARAMETER_KEY_STOP_TIME) == true) {
      final Long time = WicketUtils.getAsLong(parameters, PARAMETER_KEY_STOP_TIME);
      if (time != null) {
        form.getSearchFilter().setStopTime(new Date(time));
      } else {
        form.getSearchFilter().setStopTime(null);
      }
    }
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    final BookmarkablePageLink<Void> addTemplatesLink = UserPrefListPage.createLink("link", UserPrefArea.TIMESHEET_TEMPLATE);
    final ContentMenuEntryPanel menuEntry = new ContentMenuEntryPanel(getNewContentMenuChildId(), addTemplatesLink, getString("templates"));
    addContentMenuEntry(menuEntry);
    {
      final SubmitLink exportPDFButton = new SubmitLink(ContentMenuEntryPanel.LINK_ID, form) {
        @Override
        public void onSubmit()
        {
          exportPDF();
        };
      };
      addContentMenuEntry(new ContentMenuEntryPanel(getNewContentMenuChildId(), exportPDFButton, getString("exportAsPdf"))
      .setTooltip(getString("tooltip.export.pdf")));
    }
    {
      final SubmitLink exportExcelButton = new SubmitLink(ContentMenuEntryPanel.LINK_ID, form) {
        @Override
        public void onSubmit()
        {
          exportExcel();
        };
      };
      addContentMenuEntry(new ContentMenuEntryPanel(getNewContentMenuChildId(), exportExcelButton, getString("exportAsXls"))
      .setTooltip(getString("tooltip.export.excel")));
    }
  }

  @Override
  protected void onNextSubmit()
  {
    final ArrayList<TimesheetDO> list = new ArrayList<TimesheetDO>();
    for (final TimesheetDO sheet : getList()) {
      if (sheet.isSelected() == true) {
        list.add(sheet);
      }
    }
    setResponsePage(new TimesheetMassUpdatePage(this, list));
  }

  @Override
  public boolean isSupportsMassUpdate()
  {
    return true;
  }

  @Override
  protected void createDataTable()
  {
    final List<IColumn<TimesheetDO>> columns = createColumns(this, !isMassUpdateMode(), isMassUpdateMode(), form.getSearchFilter(),
        taskFormatter, taskTree, userFormatter, dateTimeFormatter);
    dataTable = createDataTable(columns, "startTime", SortOrder.DESCENDING);
    form.add(dataTable);
  }

  public List<IColumn<TimesheetDO>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    return createColumns(returnToPage, sortable, false, form.getSearchFilter(), taskFormatter, taskTree, userFormatter, dateTimeFormatter);
  }

  /**
   * For re-usage in other pages.
   * @param page
   * @param isMassUpdateMode
   * @param timesheetFilter If given, then the long format filter setting will be used for displaying the description, otherwise the short
   *          description is used.
   */
  @SuppressWarnings("serial")
  protected static final List<IColumn<TimesheetDO>> createColumns(final WebPage page, final boolean sortable,
      final boolean isMassUpdateMode, final TimesheetFilter timesheetFilter, final TaskFormatter taskFormatter, final TaskTree taskTree,
      final UserFormatter userFormatter, final DateTimeFormatter dateTimeFormatter)
      {
    final List<IColumn<TimesheetDO>> columns = new ArrayList<IColumn<TimesheetDO>>();
    final CellItemListener<TimesheetDO> cellItemListener = new CellItemListener<TimesheetDO>() {
      public void populateItem(final Item<ICellPopulator<TimesheetDO>> item, final String componentId, final IModel<TimesheetDO> rowModel)
      {
        final TimesheetDO timesheet = rowModel.getObject();
        final Serializable highlightedRowId;
        if (page instanceof AbstractListPage< ? , ? , ? >) {
          highlightedRowId = ((AbstractListPage< ? , ? , ? >) page).getHighlightedRowId();
        } else {
          highlightedRowId = null;
        }
        final StringBuffer cssStyle = getCssStyle(timesheet.getId(), highlightedRowId, timesheet.isDeleted());
        if (cssStyle.length() > 0) {
          item.add(AttributeModifier.append("style", new Model<String>(cssStyle.toString())));
        }
      }
    };
    if (page instanceof TimesheetMassUpdatePage) {
      columns.add(new UserPropertyColumn<TimesheetDO>(page.getString("timesheet.user"), getSortable("user.fullname", sortable), "user",
          cellItemListener).withUserFormatter(userFormatter));
    } else {
      // Show first column not for TimesheetMassUpdatePage!
      if (isMassUpdateMode == true) {
        columns.add(new CellItemListenerPropertyColumn<TimesheetDO>("", null, "selected", cellItemListener) {
          @Override
          public void populateItem(final Item<ICellPopulator<TimesheetDO>> item, final String componentId,
              final IModel<TimesheetDO> rowModel)
          {
            final TimesheetDO timesheet = rowModel.getObject();
            final CheckBoxPanel checkBoxPanel = new CheckBoxPanel(componentId, new PropertyModel<Boolean>(timesheet, "selected"), null);
            item.add(checkBoxPanel);
            cellItemListener.populateItem(item, componentId, rowModel);
            addRowClick(item, isMassUpdateMode);
          }
        });
        columns.add(new UserPropertyColumn<TimesheetDO>(page.getString("timesheet.user"), getSortable("user.fullname", sortable), "user",
            cellItemListener).withUserFormatter(userFormatter));
      } else {
        columns.add(new UserPropertyColumn<TimesheetDO>(page.getString("timesheet.user"), getSortable("user.fullname", sortable), "user",
            cellItemListener) {
          @Override
          public void populateItem(final Item<ICellPopulator<TimesheetDO>> item, final String componentId,
              final IModel<TimesheetDO> rowModel)
          {
            item.add(new ListSelectActionPanel(componentId, rowModel, TimesheetEditPage.class, rowModel.getObject().getId(), page,
                getLabelString(rowModel)));
            cellItemListener.populateItem(item, componentId, rowModel);
            addRowClick(item);
          }
        }.withUserFormatter(userFormatter));
      }
    }
    final SystemInfoCache systemInfoCache = SystemInfoCache.instance();
    if (systemInfoCache.isCost2EntriesExists() == true) {
      columns.add(new CellItemListenerPropertyColumn<TimesheetDO>(new Model<String>(page.getString("fibu.kunde")), getSortable(
          "kost2.projekt.kunde.name", sortable), "kost2.projekt.kunde.name", cellItemListener));
      columns.add(new CellItemListenerPropertyColumn<TimesheetDO>(new Model<String>(page.getString("fibu.projekt")), getSortable(
          "kost2.projekt.name", sortable), "kost2.projekt.name", cellItemListener));
    }
    columns.add(new TaskPropertyColumn<TimesheetDO>(page.getString("task"), getSortable("task.title", sortable), "task", cellItemListener)
        .withTaskFormatter(taskFormatter).withTaskTree(taskTree));
    if (systemInfoCache.isCost2EntriesExists() == true) {
      columns.add(new CellItemListenerPropertyColumn<TimesheetDO>(page.getString("fibu.kost2"), getSortable("kost2.shortDisplayName",
          sortable), "kost2.shortDisplayName", cellItemListener));
    }
    columns.add(new CellItemListenerPropertyColumn<TimesheetDO>(page.getString("calendar.weekOfYearShortLabel"), getSortable(
        "formattedWeekOfYear", sortable), "formattedWeekOfYear", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<TimesheetDO>(page.getString("calendar.dayOfWeekShortLabel"), getSortable("startTime",
        sortable), "startTime", cellItemListener) {
      @Override
      public void populateItem(final Item<ICellPopulator<TimesheetDO>> item, final String componentId, final IModel<TimesheetDO> rowModel)
      {
        final TimesheetDO timesheet = rowModel.getObject();
        final Label label = new Label(componentId, dateTimeFormatter.getFormattedDate(timesheet.getStartTime(),
            DateFormats.getFormatString(DateFormatType.DAY_OF_WEEK_SHORT)));
        cellItemListener.populateItem(item, componentId, rowModel);
        item.add(label);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<TimesheetDO>(page.getString("timePeriod"), getSortable("startTime", sortable),
        "timePeriod", cellItemListener) {
      @Override
      public void populateItem(final Item<ICellPopulator<TimesheetDO>> item, final String componentId, final IModel<TimesheetDO> rowModel)
      {
        final TimesheetDO timesheet = rowModel.getObject();
        final Label label = new Label(componentId, dateTimeFormatter.getFormattedTimePeriod(timesheet.getTimePeriod()));
        label.setEscapeModelStrings(false);
        cellItemListener.populateItem(item, componentId, rowModel);
        item.add(label);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<TimesheetDO>(page.getString("timesheet.duration"), getSortable("duration", sortable),
        "duration", cellItemListener) {
      @Override
      public void populateItem(final Item<ICellPopulator<TimesheetDO>> item, final String componentId, final IModel<TimesheetDO> rowModel)
      {
        final TimesheetDO timesheet = rowModel.getObject();
        final Label label = new Label(componentId, dateTimeFormatter.getFormattedDuration(timesheet.getDuration()));
        label.setEscapeModelStrings(false);
        cellItemListener.populateItem(item, componentId, rowModel);
        item.add(label);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<TimesheetDO>(page.getString("timesheet.location"), getSortable("location", sortable),
        "location", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<TimesheetDO>(page.getString("description"), getSortable("shortDescription", sortable),
        "shortDescription", cellItemListener) {
      @Override
      public void populateItem(final Item<ICellPopulator<TimesheetDO>> item, final String componentId, final IModel<TimesheetDO> rowModel)
      {
        final TimesheetDO timesheet = rowModel.getObject();
        final Label label = new Label(componentId, new Model<String>() {
          @Override
          public String getObject()
          {
            String text;
            if (timesheetFilter != null && timesheetFilter.isLongFormat() == true) {
              text = HtmlHelper.escapeXml(timesheet.getDescription());
            } else {
              text = HtmlHelper.escapeXml(timesheet.getShortDescription());
            }
            if (isMassUpdateMode == true) {
              return text;
            } else {
              return JiraUtils.linkJiraIssues(text); // Not in mass update mode: link on table row results otherwises in JIRA-Link.
            }
          }
        });
        label.setEscapeModelStrings(false);
        cellItemListener.populateItem(item, componentId, rowModel);
        item.add(label);
      }
    });
    return columns;
      }

  @Override
  protected TimesheetListForm newListForm(final AbstractListPage< ? , ? , ? > parentPage)
  {
    return new TimesheetListForm(this);
  }

  @Override
  protected TimesheetDao getBaseDao()
  {
    return timesheetDao;
  }

  @SuppressWarnings("serial")
  @Override
  protected IModel<TimesheetDO> getModel(final TimesheetDO object)
  {
    return new Model<TimesheetDO>() {
      @Override
      public TimesheetDO getObject()
      {
        return object;
      }
    };
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#select(java.lang.String, java.lang.Object)
   */
  @Override
  public void select(final String property, final Object selectedValue)
  {
    if ("taskId".equals(property) == true) {
      form.getSearchFilter().setTaskId((Integer) selectedValue);
      refresh();
    } else if ("userId".equals(property) == true) {
      form.getSearchFilter().setUserId((Integer) selectedValue);
      refresh();
    } else if (property.startsWith("quickSelect.") == true) { // month".equals(property) == true) {
      final Date date = (Date) selectedValue;
      form.getSearchFilter().setStartTime(date);
      final DateHolder dateHolder = new DateHolder(date);
      if (property.endsWith(".month") == true) {
        dateHolder.setEndOfMonth();
      } else if (property.endsWith(".week") == true) {
        dateHolder.setEndOfWeek();
      } else {
        log.error("Property '" + property + "' not supported for selection.");
      }
      form.getSearchFilter().setStopTime(dateHolder.getDate());
      form.startDate.markModelAsChanged();
      form.stopDate.markModelAsChanged();
      refresh();
    } else {
      super.select(property, selectedValue);
    }
  }

  /**
   * 
   * @see org.projectforge.web.fibu.ISelectCallerPage#unselect(java.lang.String)
   */
  @Override
  public void unselect(final String property)
  {
    if ("taskId".equals(property) == true) {
      form.getSearchFilter().setTaskId(null);
      refresh();
    } else if ("userId".equals(property) == true) {
      form.getSearchFilter().setUserId(null);
      refresh();
    } else {
      super.unselect(property);
    }
  }

  @Override
  public List<TimesheetDO> getList()
  {
    final TimesheetFilter filter = form.getSearchFilter();
    if (filter.getStartTime() == null && filter.getStopTime() == null && filter.getTaskId() == null) {
      return null;
    }
    return super.getList();
  }

  void exportPDF()
  {
    refresh();
    final List<TimesheetDO> timeSheets = getList();
    if (timeSheets == null || timeSheets.size() == 0) {
      // Nothing to export.
      form.addError("validation.error.nothingToExport");
      return;
    }
    final StringBuffer buf = new StringBuffer();
    buf.append("timesheets_");
    final TimesheetFilter filter = form.getSearchFilter();
    if (filter.getUserId() != null) {
      buf.append(FileHelper.createSafeFilename(userGroupCache.getUser(filter.getUserId()).getLastname(), 20)).append("_");
    }
    if (filter.getTaskId() != null) {
      final String taskTitle = taskTree.getTaskById(filter.getTaskId()).getTitle();
      buf.append(FileHelper.createSafeFilename(taskTitle, 8)).append("_");
    }
    buf.append(DateHelper.getDateAsFilenameSuffix(filter.getStartTime())).append("_")
    .append(DateHelper.getDateAsFilenameSuffix(filter.getStopTime())).append(".pdf");
    final String filename = buf.toString();

    // get the sheets from the given Format
    final String styleSheet = "/fo-styles/" + form.getExportFormat() + "/timesheet-template-fo.xsl";
    final String xmlData = "/fo-styles/" + form.getExportFormat() + "/timesheets2pdf.xml";

    // get the formatter for the different export formats
    final Formatter formatter = formatterFactory.getFormatter(form.getExportFormat());

    final Integer taskId = filter.getTaskId();

    final Map<String, Object> data = formatter.getData(timeSheets, taskId, getRequest(), getResponse(), filter);

    // render the PDF with fop
    final byte[] content = pdfRenderer.render(styleSheet, xmlData, data);

    DownloadUtils.setDownloadTarget(content, filename);
  }

  void exportExcel()
  {
    refresh();
    final List<TimesheetDO> timeSheets = getList();
    if (timeSheets == null || timeSheets.size() == 0) {
      // Nothing to export.
      form.addError("validation.error.nothingToExport");
      return;
    }
    final String filename = "ProjectForge-TimesheetExport_" + DateHelper.getDateAsFilenameSuffix(new Date()) + ".xls";
    final byte[] xls = timesheetExport.export(timeSheets);
    if (xls == null || xls.length == 0) {
      log.error("Oups, xls has zero size. Filename: " + filename);
      return;
    }
    DownloadUtils.setDownloadTarget(xls, filename);
  }

  /**
   * Avoid LazyInitializationException user.fullname.
   * @see org.projectforge.web.wicket.AbstractListPage#createSortableDataProvider(java.lang.String, boolean)
   */
  @SuppressWarnings("serial")
  @Override
  protected ISortableDataProvider<TimesheetDO> createSortableDataProvider(final String sortProperty, final SortOrder sortOrder)
  {
    return new ListPageSortableDataProvider(sortProperty, sortOrder) {
      @Override
      protected Comparator<TimesheetDO> getComparator(final String sortProperty, final boolean ascending)
      {
        return new MyBeanComparator<TimesheetDO>(sortProperty, ascending) {
          @Override
          public int compare(final TimesheetDO t1, final TimesheetDO t2)
          {
            if ("user.fullname".equals(sortProperty) == true) {
              PFUserDO user = t1.getUser();
              if (user != null && Hibernate.isInitialized(user) == false) {
                t1.setUser(userGroupCache.getUser(user.getId()));
              }
              user = t2.getUser();
              if (user != null && Hibernate.isInitialized(user) == false) {
                t2.setUser(userGroupCache.getUser(user.getId()));
              }
            } else if ("task.title".equals(sortProperty) == true) {
              TaskDO task = t1.getTask();
              if (task != null && Hibernate.isInitialized(task) == false) {
                t1.setTask(taskTree.getTaskById(task.getId()));
              }
              task = t2.getTask();
              if (task != null && Hibernate.isInitialized(task) == false) {
                t2.setTask(taskTree.getTaskById(task.getId()));
              }
            }
            return super.compare(t1, t2);
          }
        };
      }
    };
  }

  @Override
  protected String[] getBookmarkableFilterProperties()
  {
    return BOOKMARKABLE_PROPERTIES;
  }
}
