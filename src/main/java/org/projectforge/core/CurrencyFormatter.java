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

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

import org.projectforge.common.NumberHelper;
import org.projectforge.user.PFUserContext;

/**
 * Converts BigDecimal values to formatted currency strings.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
public class CurrencyFormatter
{
  /**
   * Uses the locale of the context user.
   * @param value
   * @return 1,234.00 €
   * @see #format(BigDecimal, Locale)
   */
  public static String format(final BigDecimal value)
  {
    return format(value, PFUserContext.getLocale());
  }

  /**
   * Uses the currency symbol of ConfigXml.
   * @param value
   * @param locale
   * @return 1,234.00 €
   * @see ConfigXml#getCurrencySymbol()
   */
  public static String format(final BigDecimal value, final Locale locale)
  {
    if (value == null) {
      return "";
    }
    NumberFormat nf = NumberHelper.getCurrencyFormat(locale);
    return nf.format(value) + " " + ConfigXml.getInstance().getCurrencySymbol();
  }
}
