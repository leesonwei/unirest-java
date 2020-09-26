/**
 * The MIT License
 *
 * Copyright for portions of unirest-java are held by Kong Inc (c) 2013.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package kong.tests;

import kong.unirest.Assert;
import kong.unirest.HttpMethod;
import org.junit.jupiter.api.Test;

import static kong.unirest.HttpMethod.GET;
import static kong.unirest.HttpMethod.POST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class AssertTest extends Base {

    @Test
    public void canAssertANumberOfTimes() {
        uni.get(path).asEmpty();
        uni.get(path).asEmpty();

        Assert exp = client.assertThat(HttpMethod.GET, path);
        exp.assertInvokedTimes(2);

        assertException(() -> exp.assertInvokedTimes(1),
                "Incorrect number of invocations. Expected 1 got 2\n" +
                        "GET http://basic");

        assertException(() -> exp.assertInvokedTimes(3),
                "Incorrect number of invocations. Expected 3 got 2\n" +
                        "GET http://basic");
    }

    @Test
    public void noExpectationsAtAll() {
        uni.get(path).asEmpty();
        client.verifyAll();

    }

    @Test
    public void noExpectation() {
        client.expect(GET, otherPath);
        assertException(() -> client.verifyAll(),
                "A expectation was never invoked! GET http://other\n");
    }

    @Test
    public void noInvocationHappened() {
        assertException(() -> client.assertThat(GET, path),
                "No Matching Invocation:: GET http://basic");
    }

    @Test
    public void assertHeader() {
        uni.get(path).header("monster", "grover").asEmpty();

        Assert expect = client.assertThat(GET, path);
        expect.assertHeader("monster", "grover");

        assertException(() -> expect.assertHeader("monster", "oscar"),
                "No invocation found with header [monster: oscar]\nFound:\nmonster: grover\n");
    }

    @Test
    public void canSetHeaderExpectationOnExpects() {
        client.expect(GET, path).header("monster", "grover");

        assertException(() -> client.verifyAll(),
                "A expectation was never invoked! GET http://basic\n" +
                        "Headers:\n" +
                        "monster: grover\n");

        uni.get(path).header("monster", "grover").asEmpty();

        client.verifyAll();
    }



    @Test
    public void canExpectQueryParams() {
        client.expect(GET, path).queryString("monster", "grover");

        uni.get(path).asEmpty();

        assertException(() -> client.verifyAll());

        uni.get(path).queryString("monster", "grover").asEmpty();

        client.verifyAll();
    }

    @Test
    public void expectBody() {
        client.expect(POST, path)
                .body("foo")
                .thenReturn("bar");

        assertNull(uni.post(path).asString().getBody());
        assertEquals("bar", uni.post(path).body("foo").asString().getBody());
    }

    @Test
    void assertBody() {
        client.expect(POST, path)
                .body("foo")
                .thenReturn("bar");

        uni.post(path).body("baz").asString();

        assertException(() -> client.verifyAll(),
                "A expectation was never invoked! POST http://basic\n" +
                        "Body:\n" +
                        "\tfoo");
    }
}