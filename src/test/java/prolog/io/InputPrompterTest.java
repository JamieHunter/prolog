package prolog.io;

import org.junit.jupiter.api.Test;
import prolog.flags.StreamProperties;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Deque;
import java.util.LinkedList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class InputPrompterTest {

    //
    // The Input Prompter will issue prompts if more data is required at start of line
    //

    private Deque<String> demand = new LinkedList<>();
    private FifoStreams outputFifo = new FifoStreams();
    private FifoStreams inputFifo = new FifoStreams() {
        @Override
        public void onEmpty(OutputStream stream) {
            if (!demand.isEmpty()) {
                String line = demand.removeFirst() + "\n";
                byte[] lineBytes = line.getBytes();
                try {
                    // echo the "input" to the output so we can tell prompt ordering
                    outputFifo.getOutput().write(lineBytes);
                    // make this available to the "input"
                    stream.write(line.getBytes());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    };
    private SequentialOutputStream inputFillStream = new SequentialOutputStream(inputFifo.getOutput());
    private SequentialOutputStream promptOutputStream = new SequentialOutputStream(outputFifo.getOutput());
    private SequentialInputStream outputReader = new SequentialInputStream(outputFifo.getInput());

    private InputPrompter promptStream = new InputPrompter(
            new SequentialInputStream(inputFifo.getInput()),
            promptOutputStream);
    private InputLineHandler lineStream = new InputLineHandler(
            promptStream,
            StreamProperties.NewLineMode.ATOM_detect);

    @Test
    public void testNoPromptStreamDemanded() throws IOException {
        demand.addLast("Data1");
        demand.addLast("Data2");
        lineStream.setPrompt(Prompt.NONE);
        assertThat(checkInput(), equalTo("Data1\n")); // this is the input we expected
        assertThat(checkOutput(), equalTo("Data1\n")); // this is what was "displayed" to get there
        assertThat(checkInput(), equalTo("Data2\n"));
        assertThat(checkOutput(), equalTo("Data2\n"));
    }

    @Test
    public void testNoPromptStreamPrefilled() throws IOException {
        inputFillStream.write("Data1\n");
        demand.addLast("Data2");
        lineStream.setPrompt(Prompt.NONE);
        assertThat(checkInput(), equalTo("Data1\n")); // this is the input we expected
        assertThat(checkInput(), equalTo("Data2\n"));
        assertThat(checkOutput(), equalTo("Data2\n"));
    }

    @Test
    public void testQueryPromptStreamDemanded() throws IOException {
        demand.addLast("Data1");
        demand.addLast("Data2");
        demand.addLast("Data3");
        lineStream.setPrompt(Prompt.QUERY);
        assertThat(checkInput(), equalTo("Data1\n")); // this is the input we expected
        assertThat(checkOutput(), equalTo("?- Data1\n")); // display sequence - prompt then input
        assertThat(checkInput(), equalTo("Data2\n"));
        assertThat(checkOutput(), equalTo("... Data2\n")); // continued input
        assertThat(checkInput(), equalTo("Data3\n"));
        assertThat(checkOutput(), equalTo("... Data3\n")); // continued input
    }

    @Test
    public void testQueryPromptStreamPrefilled() throws IOException {
        inputFillStream.write("Data1\n");
        demand.addLast("Data2");
        lineStream.setPrompt(Prompt.QUERY);
        assertThat(checkInput(), equalTo("Data1\n")); // this is the input we expected
        assertThat(checkOutput(), equalTo("?- \n")); // display sequence - prompt then input
        assertThat(checkInput(), equalTo("Data2\n"));
        assertThat(checkOutput(), equalTo("... Data2\n")); // continued input
    }

    @Test
    public void testConsultPromptStream() throws IOException {
        demand.addLast("Data1");
        demand.addLast("Data2");
        demand.addLast("Data3");
        lineStream.setPrompt(Prompt.CONSULT);
        assertThat(checkInput(), equalTo("Data1\n")); // this is the input we expected
        assertThat(checkOutput(), equalTo("| Data1\n")); // consult prompt
        assertThat(checkInput(), equalTo("Data2\n"));
        assertThat(checkOutput(), equalTo("| Data2\n")); // continued consult
        assertThat(checkInput(), equalTo("Data3\n"));
        assertThat(checkOutput(), equalTo("| Data3\n")); // continued consult
    }

    private String checkOutput() {
        try {
            String text = outputReader.readLine();
            if (text == null) {
                return "";
            } else {
                return text + "\n";
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String checkInput() {
        try {
            String text = lineStream.readLine();
            if (text == null) {
                return "";
            } else {
                return text + "\n";
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
