/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
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
package com.helger.pd.indexer.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.concurrent.ThreadHelper;
import com.helger.commons.mock.CommonsTestHelper;
import com.helger.commons.random.RandomHelper;
import com.helger.commons.ws.HostnameVerifierVerifyAll;
import com.helger.commons.ws.TrustManagerTrustAll;
import com.helger.pd.businesscard.PDExtendedBusinessCard;
import com.helger.pd.businesscard.v1.PD1BusinessCardType;
import com.helger.pd.businesscard.v1.PD1BusinessEntityType;
import com.helger.pd.businesscard.v1.PD1IdentifierType;
import com.helger.pd.indexer.PDIndexerTestRule;
import com.helger.pd.indexer.clientcert.ClientCertificateValidator;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.peppol.identifier.factory.PeppolIdentifierFactory;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.identifier.peppol.PeppolIdentifierHelper;
import com.helger.peppol.identifier.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.security.keystore.EKeyStoreType;
import com.helger.security.keystore.KeyStoreHelper;

/**
 * Test class for class {@link IndexerResource}.
 *
 * @author Philip Helger
 */
public final class LocalHost8080FuncTest
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (LocalHost8080FuncTest.class);

  @Rule
  public final TestRule m_aRule = new PDIndexerTestRule ();

  private WebTarget m_aTarget;

  @Nonnull
  private static PDExtendedBusinessCard _createMockBI (@Nonnull final IParticipantIdentifier aParticipantID)
  {
    final PD1BusinessCardType aBI = new PD1BusinessCardType ();
    {
      final PD1IdentifierType aID = new PD1IdentifierType ();
      aID.setScheme (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME);
      aID.setValue ("9915:mock");
      aBI.setParticipantIdentifier (aID);
    }
    {
      final PD1BusinessEntityType aEntity = new PD1BusinessEntityType ();
      aEntity.setName ("Philip's mock PEPPOL receiver");
      aEntity.setCountryCode ("AT");
      PD1IdentifierType aID = new PD1IdentifierType ();
      aID.setScheme ("mock");
      aID.setValue ("12345678");
      aEntity.addIdentifier (aID);
      aID = new PD1IdentifierType ();
      aID.setScheme ("provided");
      aID.setValue (aParticipantID.getURIEncoded ());
      aEntity.addIdentifier (aID);
      aEntity.setAdditionalInformation ("This is a mock entry for testing purposes only");
      aBI.addBusinessEntity (aEntity);
    }
    {
      final PD1BusinessEntityType aEntity = new PD1BusinessEntityType ();
      aEntity.setName ("Philip's mock PEPPOL receiver 2");
      aEntity.setCountryCode ("NO");
      final PD1IdentifierType aID = new PD1IdentifierType ();
      aID.setScheme ("mock");
      aID.setValue ("abcdefgh");
      aEntity.addIdentifier (aID);
      aEntity.setAdditionalInformation ("This is another mock entry for testing purposes only");
      aBI.addBusinessEntity (aEntity);
    }
    return new PDExtendedBusinessCard (aBI,
                                       new CommonsArrayList <> (EPredefinedDocumentTypeIdentifier.INVOICE_T010_BIS5A_V20.getAsDocumentTypeIdentifier ()));
  }

  @Before
  public void setUp () throws GeneralSecurityException, IOException
  {
    // Set test BI provider first!
    PDMetaManager.setBusinessCardProvider (aParticipantID -> _createMockBI (aParticipantID));
    PDMetaManager.getInstance ();

    final File aTestClientCertificateKeyStore = new File ("src/test/resources/smp.pilot.jks");
    if (aTestClientCertificateKeyStore.exists ())
    {
      // https
      final KeyStore aKeyStore = KeyStoreHelper.loadKeyStoreDirect (EKeyStoreType.JKS,
                                                                    aTestClientCertificateKeyStore.getAbsolutePath (),
                                                                    "peppol");
      // Try to create the socket factory from the provided key store
      final KeyManagerFactory aKeyManagerFactory = KeyManagerFactory.getInstance ("SunX509");
      aKeyManagerFactory.init (aKeyStore, "peppol".toCharArray ());

      final SSLContext aSSLContext = SSLContext.getInstance ("TLS");
      aSSLContext.init (aKeyManagerFactory.getKeyManagers (),
                        new TrustManager [] { new TrustManagerTrustAll (false) },
                        RandomHelper.getSecureRandom ());
      final Client aClient = ClientBuilder.newBuilder ()
                                          .sslContext (aSSLContext)
                                          .hostnameVerifier (new HostnameVerifierVerifyAll (false))
                                          .build ();
      m_aTarget = aClient.target ("https://localhost:8080");
    }
    else
    {
      // http only
      s_aLogger.warn ("The SMP pilot keystore is missing for the tests! Client certificate handling will not be tested!");
      ClientCertificateValidator.allowAllForTests (true);

      final Client aClient = ClientBuilder.newClient ();
      m_aTarget = aClient.target ("http://localhost:8080");
    }
  }

  @Test
  public void testCreateAndDeleteParticipant () throws IOException
  {
    final AtomicInteger aIndex = new AtomicInteger (0);
    final IParticipantIdentifier aPI_0 = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:test0");

    final int nCount = 4;
    CommonsTestHelper.testInParallel (nCount, () -> {
      // Create
      final IParticipantIdentifier aPI = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:test" +
                                                                                                                        aIndex.getAndIncrement ());

      s_aLogger.info ("PUT " + aPI.getURIEncoded ());
      final String sResponseMsg = m_aTarget.path ("1.0").request ().put (Entity.text (aPI.getURIEncoded ()),
                                                                         String.class);
      assertEquals ("", sResponseMsg);
    });

    ThreadHelper.sleep (2000);
    assertTrue (PDMetaManager.getStorageMgr ().containsEntry (aPI_0));

    aIndex.set (0);
    CommonsTestHelper.testInParallel (nCount, () -> {
      // Delete
      final IParticipantIdentifier aPI = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:test" +
                                                                                                                        aIndex.getAndIncrement ());

      s_aLogger.info ("DELETE " + aPI.getURIEncoded ());
      final String sResponseMsg = m_aTarget.path ("1.0").path (aPI.getURIEncoded ()).request ().delete (String.class);
      assertEquals ("", sResponseMsg);
    });

    ThreadHelper.sleep (2000);
    assertFalse (PDMetaManager.getStorageMgr ().containsEntry (aPI_0));
  }
}