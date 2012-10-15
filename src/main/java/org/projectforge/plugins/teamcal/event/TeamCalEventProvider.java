/////////////////////////////////////////////////////////////////////////////
//
// Project   ProjectForge
//
// Copyright 2001-2009, Micromata GmbH, Kai Reinhard
//           All rights reserved.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.plugins.teamcal.event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.ftlines.wicket.fullcalendar.Event;

import org.apache.commons.collections.CollectionUtils;
import org.apache.wicket.Component;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.projectforge.plugins.teamcal.admin.TeamCalDO;
import org.projectforge.plugins.teamcal.admin.TeamCalDao;
import org.projectforge.plugins.teamcal.admin.TeamCalRight;
import org.projectforge.plugins.teamcal.integration.TeamCalCalendarFilter;
import org.projectforge.user.PFUserContext;
import org.projectforge.user.PFUserDO;
import org.projectforge.user.UserGroupCache;
import org.projectforge.web.calendar.MyFullCalendarEventsProvider;

/**
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 * 
 */
public class TeamCalEventProvider extends MyFullCalendarEventsProvider
{

  private static final long serialVersionUID = -5609599079385073490L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TeamCalEventProvider.class);

  private UserGroupCache userGroupCache;

  private final TeamEventDao teamEventDao;

  private Integer month;

  private int days;

  private final TeamCalCalendarFilter filter;

  /**
   * the name of the event class.
   */
  public static final String EVENT_CLASS_NAME = "teamEvent";

  private final TeamCalDao teamCalDao;

  private final TeamEventRight eventRight;

  /**
   * @param parent component for i18n
   */
  public TeamCalEventProvider(final Component parent, final TeamCalDao teamCalDao, final TeamEventDao teamEventDao,
      final UserGroupCache userGroupCache, final TeamCalCalendarFilter filter)
  {
    super(parent);
    this.teamCalDao = teamCalDao;
    this.filter = filter;
    this.teamEventDao = teamEventDao;
    this.userGroupCache = userGroupCache;
    this.eventRight = new TeamEventRight();
  }

  /**
   * @see org.projectforge.web.calendar.MyFullCalendarEventsProvider#getEvents(org.joda.time.DateTime, org.joda.time.DateTime)
   */
  @Override
  public Collection<Event> getEvents(final DateTime start, final DateTime end)
  {
    final Collection<Event> events = super.getEvents(start, end);
    return events;
  }

  /**
   * @see org.projectforge.web.calendar.MyFullCalendarEventsProvider#buildEvents(org.joda.time.DateTime, org.joda.time.DateTime)
   */
  @Override
  protected void buildEvents(final DateTime start, final DateTime end)
  {
    final TeamEventFilter eventFilter = new TeamEventFilter();
    final List<TeamCalDO> selectedCalendars = filter.calcAssignedtItems(teamCalDao);
    eventFilter.setTeamCals(selectedCalendars);
    eventFilter.setStartDate(start.toDate());
    eventFilter.setEndDate(end.toDate());
    eventFilter.setUser(PFUserContext.getUser());

    final List<List<TeamEventDO>> eventLists = new ArrayList<List<TeamEventDO>>();
    if (selectedCalendars != null) {
      for (final TeamCalDO calendar : selectedCalendars) {
        eventFilter.setTeamCalId(calendar.getId());
        eventLists.add(teamEventDao.getList(eventFilter));
      }
    }

    boolean longFormat = false;
    days = Days.daysBetween(start, end).getDays();
    if (days < 10) {
      // Week or day view:
      longFormat = true;
      month = null;
      // firstDayOfMonth = null;
    } else {
      // Month view:
      final DateTime currentMonth = new DateTime(start.plusDays(10), PFUserContext.getDateTimeZone()); // Now we're definitely in the right
      // month.
      month = currentMonth.getMonthOfYear();
      // firstDayOfMonth = currentMonth.withDayOfMonth(1);
    }
    final TeamCalRight right = new TeamCalRight();
    final PFUserDO user = PFUserContext.getUser();
    if (CollectionUtils.isNotEmpty(eventLists) == true) {
      for (final List<TeamEventDO> teamEvents : eventLists) {
        for (final TeamEventDO teamEvent : teamEvents) {
          final DateTime startDate = new DateTime(teamEvent.getStartDate(), PFUserContext.getDateTimeZone());
          final DateTime endDate = new DateTime(teamEvent.getEndDate(), PFUserContext.getDateTimeZone());
          if (endDate.isBefore(start) == true || startDate.isAfter(end) == true) {
            // Event doesn't match time period start - end.
            continue;
          }

          final Event event = new Event();
          event.setClassName(EVENT_CLASS_NAME);
          event.setId("" + teamEvent.getId());
          event.setColor(filter.getColor(teamEvent.getCalendarId()));

          if (eventRight.hasUpdateAccess(PFUserContext.getUser(), teamEvent, null)) {
            event.setEditable(true);
          } else {
            event.setEditable(false);
          }

          if (teamEvent.isAllDay()) {
            event.setAllDay(true);
          }

          /*
           * necessary, because if more days are selected the calendar sets end time and start time to 00:00 thus the calendar does not
           * select the last day. for example: selected two days, but only one day would be shown.
           */
          if (endDate.getDayOfYear() != startDate.getDayOfYear()) {
            if (endDate.getMillisOfDay() == 0 && startDate.getMillisOfDay() == 0) {
              event.setAllDay(true);
            }
          } else {
            if (endDate.getMillisOfDay() == startDate.getMillisOfDay()) {
              event.setAllDay(true);
            }
          }

          event.setStart(startDate);
          event.setEnd(endDate);

          final String title = teamEvent.getSubject();
          String durationString = "";
          if (longFormat == true) {
            final DateTime dt = new DateTime(teamEvent.getDuration());
            String hour = dt.getHourOfDay() + "";
            String minute = dt.getMinuteOfHour() + "";
            if (dt.getHourOfDay() < 10)
              hour = "0" + dt.getHourOfDay();
            if (dt.getMinuteOfHour() < 10)
              minute = "0" + dt.getMinuteOfHour();

            if (right.isOwner(user, teamEvent.getCalendar()) == true
                || right.hasAccessGroup(teamEvent.getCalendar().getFullAccessGroup(), userGroupCache, user) == true) {
              if (event.isAllDay() == false)
                durationString = "\n" + getString("plugins.teamevent.duration") + ": " + hour + ":" + minute;
              event.setTitle(getString("plugins.teamevent.subject")
                  + ": "
                  + title
                  + "\n"
                  + getString("plugins.teamevent.note")
                  + ": "
                  + (teamEvent.getNote() == null ? "" : teamEvent.getNote())
                  + durationString);
            } else {
              if (right.hasAccessGroup(teamEvent.getCalendar().getReadOnlyAccessGroup(), userGroupCache, user) == true) {
                if (event.isAllDay() == false)
                  durationString = "\n" + getString("plugins.teamevent.duration") + ": " + hour + ":" + minute;
                event.setTitle(getString("plugins.teamevent.subject")
                    + ": "
                    + title
                    + "\n"
                    + getString("plugins.teamevent.note")
                    + ": "
                    + (teamEvent.getNote() == null ? "" : teamEvent.getNote())
                    + durationString);
                event.setEditable(false);
              } else {
                // for minimal access
                event.setTitle("");
                event.setEditable(false);
              }
            }
          } else {
            // Month view:
            if (right.isOwner(user, teamEvent.getCalendar()) == true
                || right.hasAccessGroup(teamEvent.getCalendar().getFullAccessGroup(), userGroupCache, user) == true
                || right.hasAccessGroup(teamEvent.getCalendar().getReadOnlyAccessGroup(), userGroupCache, user) == true) {
              event.setTitle(title);
            } else {
              event.setTitle("");
            }
          }
          events.put(teamEvent.getId() + "", event);
        }
      }
    }
  }

  public void setUserGroupCache(final UserGroupCache userGroupCache)
  {
    this.userGroupCache = userGroupCache;
  }

  /**
   * @return the log
   */
  public static org.apache.log4j.Logger getLog()
  {
    return log;
  }

  /**
   * @param selectedCalendar
   * @return
   */
  public static TeamCalDO getTeamCalForEncodedId(final TeamCalDao teamCalDao, String selectedCalendar)
  {
    if (selectedCalendar == null) {
      return null;
    }
    if (selectedCalendar.contains("-")) {
      selectedCalendar = selectedCalendar.substring(selectedCalendar.indexOf("-") + 1);
    }
    try {
      return teamCalDao.getById(Integer.valueOf(selectedCalendar));
    } catch (final NumberFormatException ex) {
      log.warn("Unable to get teamCalDao for id " + selectedCalendar);
    }
    return null;
  }

}
