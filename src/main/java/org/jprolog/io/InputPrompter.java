// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.io;

import java.io.IOException;

/**
 * Intercepts prompt request (for TTY)
 */
public class InputPrompter extends FilteredInputStream {

    private Prompt prompt = Prompt.NONE;
    private final PrologOutputStream outputStream;

    public InputPrompter(PrologInputStream stream, PrologOutputStream outputStream) {
        super(stream);
        this.outputStream = outputStream;
    }

    @Override
    public void setPrompt(Prompt prompt) {
        this.prompt = prompt;
    }

    @Override
    public void showPrompt() throws IOException {
        String text = prompt.text();
        if (text != null) {
            outputStream.write(text);
            outputStream.flush();
            prompt = prompt.next();
        }
    }
}
