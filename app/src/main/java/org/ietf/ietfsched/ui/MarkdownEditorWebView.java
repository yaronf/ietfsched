/*
 * Copyright 2025 IETF
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ietf.ietfsched.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Secure WebView wrapper for Vditor Markdown Editor.
 * Provides a markdown editor with WYSIWYG interface.
 */
public class MarkdownEditorWebView extends WebView {
    private static final String TAG = "MarkdownEditorWebView";
    private EditorListener listener;
    private boolean isEditorReady = false;
    
    public interface EditorListener {
        void onEditorReady();
        void onContentChanged();
        void onShareClicked();
    }
    
    public MarkdownEditorWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Enable WebView debugging (can be disabled in production)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        // Make WebView focusable for input
        setFocusable(true);
        setFocusableInTouchMode(true);
        // Enable hardware acceleration for better input handling
        setLayerType(LAYER_TYPE_HARDWARE, null);
        setupSecureWebView();
    }
    
    private void setupSecureWebView() {
        WebSettings settings = getSettings();
        
        // Enable JavaScript (required for editor)
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        
        // Enable text selection and input
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        
        // SECURITY: Enable file access for local assets only
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(true);  // Allow XHR to file:// from file://
        settings.setAllowUniversalAccessFromFileURLs(false);
        
        // SECURITY: Disable other features
        settings.setDatabaseEnabled(false);
        settings.setGeolocationEnabled(false);
        
        // Enable input methods
        requestFocus(View.FOCUS_DOWN);
        
        // Add JavaScript interface
        addJavascriptInterface(new JavaScriptBridge(), "Android");
        
        setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "Editor loaded");
                // Focus the editor after loading
                view.loadUrl("javascript:if(typeof vditor !== 'undefined') { vditor.focus(); }");
            }
        });
        
        // Capture console logs for debugging
        setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage cm) {
                Log.d(TAG, String.format("[JS] %s:%d - %s", 
                    cm.sourceId(), cm.lineNumber(), cm.message()));
                return true;
            }
        });
        
        loadUrl("file:///android_asset/vditor/editor.html");
    }
    
    public void setEditorListener(EditorListener listener) {
        this.listener = listener;
    }
    
    /**
     * Request focus on the editor
     */
    public void focusEditor() {
        if (isEditorReady) {
            loadUrl("javascript:if(typeof vditor !== 'undefined') { vditor.focus(); }");
        }
    }
    
    /**
     * Get markdown content from editor.
     */
    public void getMarkdown(MarkdownCallback callback) {
        if (!isEditorReady) return;
        
        evaluateJavascript(
            "window.EditorBridge.getMarkdown()",
            value -> {
                // Remove JSON quotes and unescape
                String markdown = value
                    .replaceAll("^\"(.*)\"$", "$1")
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
                callback.onMarkdown(markdown);
            }
        );
    }
    
    /**
     * Set markdown content in editor.
     */
    public void setMarkdown(String markdown) {
        if (!isEditorReady || markdown == null) return;
        
        // Escape for JavaScript string
        String escaped = markdown
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "");
        
        evaluateJavascript(
            "window.EditorBridge.setMarkdown(\"" + escaped + "\")",
            null
        );
    }
    
    /**
     * Initialize editor with H1 header containing area and group.
     */
    public void initWithHeader(String area, String group) {
        if (!isEditorReady) return;
        
        // Basic escaping for JS
        area = (area != null) ? area.replace("\"", "\\\"") : "";
        group = (group != null) ? group.replace("\"", "\\\"") : "";
        
        evaluateJavascript(
            "window.EditorBridge.initWithHeader(\"" + area + "\", \"" + 
            group + "\")",
            null
        );
    }
    
    public interface MarkdownCallback {
        void onMarkdown(String markdown);
    }
    
    /**
     * JavaScript bridge - minimal API surface for security.
     */
    private class JavaScriptBridge {
        @JavascriptInterface
        public void onEditorReady() {
            isEditorReady = true;
            if (listener != null) {
                post(() -> listener.onEditorReady());
            }
        }
        
        @JavascriptInterface
        public void onContentChanged() {
            if (listener != null) {
                post(() -> listener.onContentChanged());
            }
        }
        
        @JavascriptInterface
        public void onShareClicked() {
            if (listener != null) {
                post(() -> listener.onShareClicked());
            }
        }
    }
}
