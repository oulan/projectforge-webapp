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

package org.projectforge.core;

import org.hibernate.search.bridge.StringBridge;
import org.projectforge.common.NumberHelper;
import org.projectforge.common.StringHelper;

/**
 * StringBridge for hibernate search to search in phone numbers (reduce phone number fields to digits without white spaces and non digits).
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class HibernateSearchPhoneNumberBridge implements StringBridge
{
  public String objectToString(final Object object)
  {
    if (object == null || object instanceof String == false) {
      return "";
    }
    final String number = (String) object;
    return number + '|' + StringHelper.removeNonDigits(number) + '|' + NumberHelper.extractPhonenumber(number);
  }
}
