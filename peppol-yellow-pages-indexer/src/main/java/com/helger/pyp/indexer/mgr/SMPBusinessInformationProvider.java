/**
 * Copyright (C) 2015 Philip Helger (www.helger.com)
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
package com.helger.pyp.indexer.mgr;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.busdox.servicemetadata.publishing._1.ExtensionType;
import org.busdox.servicemetadata.publishing._1.ServiceGroupType;
import org.busdox.servicemetadata.publishing._1.ServiceMetadataReferenceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.microdom.IMicroElement;
import com.helger.commons.microdom.IMicroNode;
import com.helger.commons.microdom.serialize.MicroWriter;
import com.helger.commons.microdom.util.MicroHelper;
import com.helger.commons.url.SimpleURL;
import com.helger.commons.xml.XMLDebug;
import com.helger.peppol.identifier.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.IdentifierHelper;
import com.helger.peppol.identifier.doctype.SimpleDocumentTypeIdentifier;
import com.helger.peppol.identifier.participant.IPeppolParticipantIdentifier;
import com.helger.peppol.smpclient.SMPClientReadOnly;
import com.helger.peppol.smpclient.exception.SMPClientException;
import com.helger.pyp.businessinformation.BusinessInformationType;
import com.helger.pyp.businessinformation.IPYPBusinessInformationProvider;
import com.helger.pyp.businessinformation.PYPBusinessInformationMarshaller;
import com.helger.pyp.businessinformation.PYPExtendedBusinessInformation;
import com.helger.pyp.settings.PYPSettings;

/**
 * The SMP based {@link IPYPBusinessInformationProvider} implementation. An SMP
 * lookup of the ServiceGroup is performed, and the <code>Extension</code>
 * element is parsed for the elements as specified in the PYP specification.
 *
 * @author Philip Helger
 */
public class SMPBusinessInformationProvider implements IPYPBusinessInformationProvider
{
  private static final String URL_PART_SERVICES = "/services/";
  private static final Logger s_aLogger = LoggerFactory.getLogger (SMPBusinessInformationProvider.class);

  @Nullable
  public static BusinessInformationType extractBusinessInformation (@Nullable final ExtensionType aExtension)
  {
    if (aExtension != null && aExtension.getAny () != null)
    {
      final IMicroNode aExtensionContainer = MicroHelper.convertToMicroNode (aExtension.getAny ());
      if (aExtensionContainer instanceof IMicroElement)
      {
        final IMicroElement eExtensionContainer = (IMicroElement) aExtensionContainer;
        if ("ExtensionContainer".equals (eExtensionContainer.getTagName ()))
        {
          for (final IMicroElement eExtensionElement : eExtensionContainer.getAllChildElements ("ExtensionElement"))
            if ("business information".equals (eExtensionElement.getAttributeValue ("type")))
            {
              final IMicroElement eBussinessInfo = eExtensionElement.getFirstChildElement ("BusinessInformation");
              if (eBussinessInfo != null)
              {
                final String sBusinessInfo = MicroWriter.getXMLString (eBussinessInfo);
                final BusinessInformationType aBI = new PYPBusinessInformationMarshaller ().read (sBusinessInfo);
                if (aBI != null)
                {
                  // Finally we're done
                  return aBI;
                }
                s_aLogger.warn ("Failed to parse business information data:\n" + sBusinessInfo);
              }
              else
                s_aLogger.warn ("The 'ExtensionElement' for business information does not contain a 'BusinessInformation' child element");
              break;
            }
          s_aLogger.warn ("'ExtensionContainer' does not contain an 'ExtensionElement' with @type 'business information'");
        }
        else
        {
          s_aLogger.warn ("Extension content is expected to be an 'ExtensionContainer' but it is a '" +
                          eExtensionContainer.getTagName () +
                          "'");
        }
      }
      else
      {
        s_aLogger.warn ("Extension content is not an element but a " +
                        XMLDebug.getNodeTypeAsString (aExtension.getAny ().getNodeType ()));
      }
    }

    return null;
  }

  @Nullable
  public PYPExtendedBusinessInformation getBusinessInformation (@Nonnull final IPeppolParticipantIdentifier aParticipantID)
  {
    // Fetch data
    final SMPClientReadOnly aSMPClient = new SMPClientReadOnly (aParticipantID, PYPSettings.getSMLToUse ());
    ServiceGroupType aServiceGroup;
    try
    {
      aServiceGroup = aSMPClient.getServiceGroup (aParticipantID);
    }
    catch (final SMPClientException ex)
    {
      s_aLogger.error ("Error querying SMP for service group '" + aParticipantID.getURIEncoded () + "'", ex);
      return null;
    }

    final BusinessInformationType aBI = extractBusinessInformation (aServiceGroup.getExtension ());
    if (aBI == null)
    {
      // No extension present - no need to try again
      s_aLogger.warn ("Failed to get SMP BusinessInformation from Extension of service group " +
                      aParticipantID.getURIEncoded ());
      return null;
    }

    final List <IDocumentTypeIdentifier> aDocumentTypeIDs = new ArrayList <> ();
    for (final ServiceMetadataReferenceType aRef : aServiceGroup.getServiceMetadataReferenceCollection ()
                                                                .getServiceMetadataReference ())
    {
      // Extract the path in case there are parameters or anchors attached
      final String sHref = new SimpleURL (aRef.getHref ()).getPath ();
      final int nIndex = sHref.indexOf (URL_PART_SERVICES);
      if (nIndex < 0)
      {
        s_aLogger.error ("Invalid href when querying service group '" +
                         aParticipantID.getURIEncoded () +
                         "': '" +
                         sHref +
                         "'");
      }
      else
      {
        final String sDocumentTypeID = sHref.substring (nIndex + URL_PART_SERVICES.length ());
        final SimpleDocumentTypeIdentifier aDocTypeID = IdentifierHelper.createDocumentTypeIdentifierFromURIPartOrNull (sDocumentTypeID);
        if (aDocTypeID == null)
        {
          s_aLogger.error ("Invalid document type when querying service group '" +
                           aParticipantID.getURIEncoded () +
                           "': '" +
                           aDocTypeID +
                           "'");
        }
        else
        {
          // Success
          aDocumentTypeIDs.add (aDocTypeID);
        }
      }
    }

    return new PYPExtendedBusinessInformation (aBI, aDocumentTypeIDs);
  }
}
