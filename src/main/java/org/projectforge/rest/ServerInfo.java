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

package org.projectforge.rest;

import org.projectforge.user.PFUserDO;

/**
 * REST object for system info for initial contact.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class ServerInfo
{
  public static final String STATUS_OK = "OK";

  public static final String STATUS_CLIENT_TO_OLD = "CLIENT_TO_OLD";

  public static final String STATUS_CLIENT_NEWER_THAN_SERVER = "STATUS_CLIENT_NEWER_THAN_SERVER";

  public static final String STATUS_UNKNOWN = "STATUS_UNKOWN";

  private final String version;

  private final UserObject user;

  private String status;

  public ServerInfo(final PFUserDO user, final String version)
  {
    this.user = new UserObject(user);
    this.version = version;
  }

  public String getVersion()
  {
    return version;
  }

  public UserObject getUser()
  {
    return user;
  }

  /**
   * @return the status of the client server connection.
   */
  public String getStatus()
  {
    return status;
  }

  /**
   * @param status the status to set
   * @return this for chaining.
   */
  public void setStatus(final String status)
  {
    this.status = status;
  }
}