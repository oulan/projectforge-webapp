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

package org.projectforge.web.calendar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.projectforge.access.AccessChecker;
import org.projectforge.address.AddressDao;
import org.projectforge.address.BirthdayAddress;
import org.projectforge.calendar.DayHolder;
import org.projectforge.calendar.MonthHolder;
import org.projectforge.calendar.WeekHolder;
import org.projectforge.common.DateHolder;
import org.projectforge.core.OrderDirection;
import org.projectforge.timesheet.TimesheetDO;
import org.projectforge.timesheet.TimesheetDao;
import org.projectforge.timesheet.TimesheetFilter;
import org.projectforge.user.ProjectForgeGroup;
import org.projectforge.web.timesheet.OldDisplayTimesheet;


@Deprecated
public class OldCalendarMonthHolder extends MonthHolder
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(OldCalendarMonthHolder.class);

  private final CalendarFilter calFilter;

  private AccessChecker accessChecker;

  private TimesheetDao timesheetDao;

  private AddressDao addressDao;

  private long monthDuration;

  public OldCalendarMonthHolder(final CalendarFilter calFilter)
  {
    super(new Date());//calFilter.getCurrent());
    this.calFilter = calFilter;
  }

  @SuppressWarnings("unchecked")
  protected void init()
  {
    final DayHolder firstDay = getFirstWeek().getFirstDay(); // Is often the last sunday / monday of the previous month.
    final DayHolder lastDay = getLastWeek().getLastDay(); // Is often the first monday / sunday of the following month.
    if (calFilter.getUserId() != null) {
      final TimesheetFilter filter = new TimesheetFilter();
      filter.setUserId(calFilter.getUserId());
      filter.setStartTime(firstDay.getDate());
      filter.setStopTime(lastDay.getDate());
      filter.setOrderType(OrderDirection.ASC);
      final List<TimesheetDO> timesheets = timesheetDao.getList(filter);
      monthDuration = 0;
      if (CollectionUtils.isNotEmpty(timesheets) == true) {
        final Iterator<TimesheetDO> it = timesheets.iterator();
        TimesheetDO timesheet = it.next(); // Start with first time sheet
        Date lastStoptime = null;
        OldDisplayTimesheet last = null;
        for (final WeekHolder week : getWeeks()) {
          long weekDuration = 0;
          for (final DayHolder day : week.getDays()) {
            long dayDuration = 0;
            Collection<OldDisplayTimesheet> col = null;
            while (day.isSameDay(timesheet.getStartTime()) == true) {
              if (col == null) {
                col = new ArrayList<OldDisplayTimesheet>();
                day.addObject("timesheets", col);
              }
              final OldDisplayTimesheet cur = new OldDisplayTimesheet(timesheet);
              if (lastStoptime != null) {
                final DayHolder d = new DayHolder(lastStoptime);
                if (d.isSameDay(timesheet.getStartTime()) == true) {
                  if (lastStoptime.before(timesheet.getStartTime()) == true) {
                    // Create empty entry (may be pause):
                    col.add(OldDisplayTimesheet.createBreak(lastStoptime, timesheet.getStartTime()));
                  } else {
                    // The stop time of the last time sheet is equals to start time of current, so do not display link for this time stamp
                    // (the user
                    // can't have a third time sheet without overlap with this time stamp).
                    cur.setStartTimeLinkEnabled(false);
                    last.setStopTimeLinkEnabled(false);
                  }
                }
              }
              col.add(cur);
              dayDuration += timesheet.getDuration();
              if (it.hasNext() == false) {
                break;
              }
              lastStoptime = timesheet.getStopTime();
              last = cur;
              timesheet = it.next();
            }
            day.addObject("duration", dayDuration);
            weekDuration += dayDuration;
            if (containsDay(day) == true) {
              monthDuration += dayDuration;
            }
          }
          if (weekDuration > 0) {
            week.addObject("duration", weekDuration);
          }
        }
      }
    } // if (settings.isShowTimesheets() == true)
    if (calFilter.isShowBirthdays() == true) {
      // February, 29th 2010 fix:
      Date from = firstDay.getDate();
      if (firstDay.getMonth() == Calendar.MARCH && firstDay.getDayOfMonth() == 1) {
        final DateHolder dh = new DateHolder(firstDay.getDate());
        dh.add(Calendar.DAY_OF_MONTH, -1); // Take birthday from February 29th into March, 1st.
        from = dh.getDate();
      }
      final Set<BirthdayAddress> set = addressDao.getBirthdays(from, lastDay.getDate(), 100, true);
      if (CollectionUtils.isNotEmpty(set) == true) {
        Collection<BirthdayAddress> col = null;
        DayHolder day;
        for (final BirthdayAddress ba : set) {
          day = getDay(ba.getMonth(), ba.getDayOfMonth());
          if (day == null) { // February, 29th fix:
            if (ba.getMonth() == Calendar.FEBRUARY && ba.getDayOfMonth() == 29) {
              day = getDay(Calendar.MARCH, 1);
            } else {
              log.warn("Oups, day not found in MonthHolder: " + ba.getCompareString());
            }
          }
          if (day == null) {
            continue;
          }
          col = (Collection<BirthdayAddress>) day.getObject("birthdays");
          if (col == null) {
            col = new ArrayList<BirthdayAddress>();
            day.addObject("birthdays", col);
          }
          if (accessChecker.isLoggedInUserMemberOfGroup(ProjectForgeGroup.FINANCE_GROUP) == true) {
            ba.setAge(day.getDate());
          }
          col.add(ba);
        }
      }
    }
  }

  public void setAccessChecker(final AccessChecker accessChecker)
  {
    this.accessChecker = accessChecker;
  }

  public void setAddressDao(final AddressDao addressDao)
  {
    this.addressDao = addressDao;
  }

  public void setTimesheetDao(final TimesheetDao timesheetDao)
  {
    this.timesheetDao = timesheetDao;
  }

  public long getMonthDuration()
  {
    return monthDuration;
  }
}
