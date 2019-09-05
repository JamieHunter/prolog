package prolog.io;

import org.junit.jupiter.api.Test;
import prolog.flags.StreamProperties;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class InputLineHandlerTest {

    //
    // The InputLineHandler is looking for what looks like end of lines, and establishing
    // a prompt if a prompt handler exists.
    //

    private static String testString = "One\nTwo\n\nThree\rFour\r\nFive\n\rSix";
    private static byte[] testData = testString.getBytes();

    private SequentialInputStream sourceStream = new SequentialInputStream(new ByteArrayInputStream(testData));

    private InputLineHandler detectingStream = new InputLineHandler(
            sourceStream,
            StreamProperties.NewLineMode.ATOM_detect);

    private InputLineHandler dosStream = new InputLineHandler(
            sourceStream,
            StreamProperties.NewLineMode.ATOM_dos);

    private InputLineHandler posixStream = new InputLineHandler(
            sourceStream,
            StreamProperties.NewLineMode.ATOM_posix);

    @Test
    public void testDetectingStream() throws IOException {
        String line = detectingStream.readLine();
        assertThat(line, equalTo("One"));
        line = detectingStream.readLine();
        assertThat(line, equalTo("Two"));
        line = detectingStream.readLine();
        assertThat(line, equalTo("")); // two new-lines
        line = detectingStream.readLine();
        assertThat(line, equalTo("Three\rFour")); // CR not considered line terminator
        line = detectingStream.readLine();
        assertThat(line, equalTo("Five")); // START_OF_LINE (CR not parsed)
        line = detectingStream.readLine();
        assertThat(line, equalTo("\rSix")); // CR not considered line terminator
        line = detectingStream.readLine();
        assertThat(line, is(nullValue()));
        line = detectingStream.readLine();
        assertThat(line, is(nullValue()));
    }

    @Test
    public void testDosStream() throws IOException {
        // This filter recognizes \r\n or \n as end of line
        String line = dosStream.readLine();
        assertThat(line, equalTo("One"));
        line = dosStream.readLine();
        assertThat(line, equalTo("Two"));
        line = dosStream.readLine();
        assertThat(line, equalTo(""));
        line = dosStream.readLine();
        assertThat(line, equalTo("Three\rFour"));
        line = dosStream.readLine();
        assertThat(line, equalTo("Five"));
        line = dosStream.readLine();
        assertThat(line, equalTo("\rSix"));
        line = dosStream.readLine();
        assertThat(line, is(nullValue()));
        line = dosStream.readLine();
        assertThat(line, is(nullValue()));
    }

    @Test
    public void testPosixStream() throws IOException {
        String line = posixStream.readLine();
        assertThat(line, equalTo("One"));
        line = posixStream.readLine();
        assertThat(line, equalTo("Two"));
        line = posixStream.readLine();
        assertThat(line, equalTo("")); // two new-lines
        line = posixStream.readLine();
        assertThat(line, equalTo("Three\rFour\r"));
        line = posixStream.readLine();
        assertThat(line, equalTo("Five"));
        line = posixStream.readLine();
        assertThat(line, equalTo("\rSix"));
        line = posixStream.readLine();
        assertThat(line, is(nullValue()));
        line = posixStream.readLine();
        assertThat(line, is(nullValue()));
    }

}
