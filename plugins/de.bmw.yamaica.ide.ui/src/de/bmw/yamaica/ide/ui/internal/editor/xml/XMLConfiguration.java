/* Copyright (C) 2013-2015 BMW Group
 * Author: Manfred Bathelt (manfred.bathelt@bmw.de)
 * Author: Juergen Gehring (juergen.gehring@bmw.de)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package de.bmw.yamaica.ide.ui.internal.editor.xml;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

public class XMLConfiguration extends SourceViewerConfiguration
{
    private XMLDoubleClickStrategy doubleClickStrategy;
    private XMLTagScanner          tagScanner;
    private XMLScanner             scanner;
    private ColorManager           colorManager;

    public XMLConfiguration(ColorManager colorManager)
    {
        this.colorManager = colorManager;
    }

    public String[] getConfiguredContentTypes(ISourceViewer sourceViewer)
    {
        return new String[] { IDocument.DEFAULT_CONTENT_TYPE, XMLPartitionScanner.XML_COMMENT, XMLPartitionScanner.XML_TAG };
    }

    public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType)
    {
        if (doubleClickStrategy == null)
            doubleClickStrategy = new XMLDoubleClickStrategy();
        return doubleClickStrategy;
    }

    protected XMLScanner getXMLScanner()
    {
        if (scanner == null)
        {
            scanner = new XMLScanner(colorManager);
            scanner.setDefaultReturnToken(new Token(new TextAttribute(colorManager.getColor(IXMLColorConstants.DEFAULT))));
        }
        return scanner;
    }

    protected XMLTagScanner getXMLTagScanner()
    {
        if (tagScanner == null)
        {
            tagScanner = new XMLTagScanner(colorManager);
            tagScanner.setDefaultReturnToken(new Token(new TextAttribute(colorManager.getColor(IXMLColorConstants.TAG))));
        }
        return tagScanner;
    }

    public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer)
    {
        PresentationReconciler reconciler = new PresentationReconciler();

        DefaultDamagerRepairer dr = new DefaultDamagerRepairer(getXMLTagScanner());
        reconciler.setDamager(dr, XMLPartitionScanner.XML_TAG);
        reconciler.setRepairer(dr, XMLPartitionScanner.XML_TAG);

        dr = new DefaultDamagerRepairer(getXMLScanner());
        reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
        reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

        NonRuleBasedDamagerRepairer ndr = new NonRuleBasedDamagerRepairer(new TextAttribute(
                colorManager.getColor(IXMLColorConstants.XML_COMMENT)));
        reconciler.setDamager(ndr, XMLPartitionScanner.XML_COMMENT);
        reconciler.setRepairer(ndr, XMLPartitionScanner.XML_COMMENT);

        return reconciler;
    }

}
