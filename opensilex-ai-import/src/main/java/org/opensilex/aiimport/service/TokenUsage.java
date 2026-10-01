//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service;

import org.opensilex.aiimport.service.dto.ChatResponse;

/**
 * What a conversation has actually cost, as the endpoint reports it.
 * <p>
 * The only figure here that is not an estimate. A character count divided by four is fine for
 * catching a regression at review time; it is not fine for telling an administrator what their
 * instance spent.
 * <p>
 * Counted per API call rather than per user message, because one message can be several calls: the
 * assistant looks something up, reads the result, and answers. `calls` divided by `messages` is what
 * says how chatty the tool loop is being.
 *
 * @author Arnaud Charleroy
 */
public class TokenUsage {

    private long promptTokens;
    private long completionTokens;
    private int calls;

    /**
     * Adds one response's usage. A response that reports none is still counted as a call, so the
     * ratio of calls to messages stays honest on endpoints that omit the field.
     */
    /**
     * Puts back the counters of a stored session, so its cost keeps adding up across a resumption.
     */
    public synchronized void restore(long promptTokens, long completionTokens, int calls) {
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.calls = calls;
    }

    public synchronized void add(ChatResponse response) {
        calls++;
        if (response == null || response.getUsage() == null) {
            return;
        }
        promptTokens += response.getUsage().getPromptTokens();
        completionTokens += response.getUsage().getCompletionTokens();
    }

    public synchronized long getPromptTokens() {
        return promptTokens;
    }

    public synchronized long getCompletionTokens() {
        return completionTokens;
    }

    public synchronized long getTotalTokens() {
        return promptTokens + completionTokens;
    }

    public synchronized int getCalls() {
        return calls;
    }

    @Override
    public synchronized String toString() {
        return calls + " call(s), " + promptTokens + " prompt + " + completionTokens
                + " completion = " + getTotalTokens() + " tokens";
    }
}
