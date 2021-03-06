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

package org.projectforge.web.rest;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.projectforge.common.NumberHelper;
import org.projectforge.user.PFUserContext;
import org.projectforge.user.PFUserDO;
import org.projectforge.user.UserDao;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Does the authentication stuff for restfull requests.
 * @author Daniel Ludwig (d.ludwig@micromata.de)
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class RestUserFilter implements Filter
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(RestUserFilter.class);

  static final String AUTHENTICATION_USER_ID = "authenticationUserId";

  static final String AUTHENTICATION_TOKEN = "authenticationToken";

  static final String AUTHENTICATION_USERNAME = "authenticationUsername";

  static final String AUTHENTICATION_PASSWORD = "authenticationPassword";

  @Autowired
  UserDao userDao;

  @Override
  public void init(final FilterConfig filterConfig) throws ServletException
  {
    // NOOP
  }

  /**
   * Authentication via request header.
   * <ol>
   * <li>Authentication userId (authenticationUserId) and authenticationToken (authenticationToken) or</li>
   * <li>Authentication username (authenticationUsername) and password (authenticationPassword) or</li>
   * </ol>
   * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
   */
  @Override
  public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException,
  ServletException
  {
    final HttpServletRequest req = (HttpServletRequest) request;
    String userString = req.getHeader(AUTHENTICATION_USER_ID);
    PFUserDO user = null;
    if (userString != null) {
      final Integer userId = NumberHelper.parseInteger(userString);
      if (userId != null) {
        final String authenticationToken = req.getHeader(AUTHENTICATION_TOKEN);
        if (authenticationToken != null) {
          if (authenticationToken.equals(userDao.getCachedAuthenticationToken(userId)) == true) {
            user = userDao.getUserGroupCache().getUser(userId);
          } else {
            log.error(AUTHENTICATION_TOKEN + " doesn't match for " + AUTHENTICATION_USER_ID + " '" + userId + "'. Rest call forbidden.");
          }
        } else {
          log.error(AUTHENTICATION_TOKEN + " not given for userId '" + userId + "'. Rest call forbidden.");
        }
      } else {
        log.error(AUTHENTICATION_USER_ID + " is not an integer: '" + userString + "'. Rest call forbidden.");
      }
    } else {
      userString = req.getHeader(AUTHENTICATION_USERNAME);
      final String password = req.getHeader(AUTHENTICATION_PASSWORD);
      if (userString != null && password != null) {
        final String encryptedPassword = userDao.encryptPassword(password);
        user = userDao.authenticateUser(userString, encryptedPassword);
        if (user == null) {
          log.error("Authentication failed for "
              + AUTHENTICATION_USERNAME
              + "='"
              + userString
              + "' with given password. Rest call forbidden.");
        }
      } else {
        log.error("Neither "
            + AUTHENTICATION_USER_ID
            + " nor "
            + AUTHENTICATION_USERNAME
            + "/"
            + AUTHENTICATION_PASSWORD
            + " is given. Rest call forbidden.");
      }
    }
    if (user == null) {
      try {
        // Avoid brute force attack:
        Thread.sleep(1000);
      } catch (final InterruptedException ex) {
        log.fatal("Exception encountered while Thread.sleep(1000): " + ex, ex);
      }
      final HttpServletResponse resp = (HttpServletResponse) response;
      resp.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }
    try {
      PFUserContext.setUser(user);
      chain.doFilter(request, response);
    } finally {
      PFUserContext.setUser(null);
    }
  }

  @Override
  public void destroy()
  {
    // NOOP
  }

  public UserDao getUserDao()
  {
    return userDao;
  }

  public void setUserDao(final UserDao userDao)
  {
    this.userDao = userDao;
  }

}
