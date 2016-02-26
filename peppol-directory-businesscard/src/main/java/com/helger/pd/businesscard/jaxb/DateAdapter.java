/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.pd.businesscard.jaxb;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.helger.commons.annotation.PresentForCodeCoverage;
import com.helger.datetime.format.PDTFromString;

/**
 * This class is used for converting between XML time elements and Java Date
 * objects. It is internally used in the JAXB bindings.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
@Immutable
public final class DateAdapter
{
  @PresentForCodeCoverage
  private static final DateAdapter s_aInstance = new DateAdapter ();

  private DateAdapter ()
  {}

  @Nullable
  public static LocalDate getLocalDateFromXSD (@Nullable final String sValue)
  {
    final DateTimeFormatter aDTF = ISODateTimeFormat.dateParser ();
    return PDTFromString.getLocalDateFromString (sValue, aDTF);
  }

  @Nullable
  public static String getLocalDateAsStringXSD (@Nullable final LocalDate aLocalDate)
  {
    return aLocalDate == null ? null : ISODateTimeFormat.date ().withOffsetParsed ().print (aLocalDate);
  }
}
