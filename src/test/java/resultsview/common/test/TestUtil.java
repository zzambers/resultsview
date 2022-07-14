/*
 * The MIT License
 *
 * Copyright 2022 zzambers.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package resultsview.common.test;

import org.junit.Assert;
import org.junit.Test;
import resultsview.common.VersionUtil;

public class TestUtil {

    @Test
    public void testVersionCompare() {
        int cmp;
        cmp = VersionUtil.versionCompare("a", "a");
        Assert.assertTrue(cmp == 0);
        cmp = VersionUtil.versionCompare("a", "b");
        Assert.assertTrue(cmp < 0);
        cmp = VersionUtil.versionCompare("b", "a");
        Assert.assertTrue(cmp > 0);
        
        cmp = VersionUtil.versionCompare("abc1def", "abc1deg");
        Assert.assertTrue(cmp < 0);
        cmp = VersionUtil.versionCompare("abc1deg", "abc1def");
        Assert.assertTrue(cmp > 0);
        
        cmp = VersionUtil.versionCompare("ab", "a");
        Assert.assertTrue(cmp > 0);
        cmp = VersionUtil.versionCompare("a", "ab");
        Assert.assertTrue(cmp < 0);

        cmp = VersionUtil.versionCompare("1", "2");
        Assert.assertTrue(cmp < 0);
        cmp = VersionUtil.versionCompare("01", "2");
        Assert.assertTrue(cmp < 0);
        cmp = VersionUtil.versionCompare("2", "1");
        Assert.assertTrue(cmp > 0);
        cmp = VersionUtil.versionCompare("2", "01");
        Assert.assertTrue(cmp > 0);
        
        cmp = VersionUtil.versionCompare("1", "1");
        Assert.assertTrue(cmp == 0);
        cmp = VersionUtil.versionCompare("1", "01");
        Assert.assertTrue(cmp == 0);
        
        cmp = VersionUtil.versionCompare("2", "10");
        Assert.assertTrue(cmp < 0);
        cmp = VersionUtil.versionCompare("10", "2");
        Assert.assertTrue(cmp > 0);
        cmp = VersionUtil.versionCompare("0000000000000000000000000000000000000002", "10");
        Assert.assertTrue(cmp < 0);
        cmp = VersionUtil.versionCompare("10", "0000000000000000000000000000000000000002");
        Assert.assertTrue(cmp > 0); 
        
        cmp = VersionUtil.versionCompare("a1", "a");
        Assert.assertTrue(cmp > 0);
        cmp = VersionUtil.versionCompare("a", "a1");
        Assert.assertTrue(cmp < 0);

        cmp = VersionUtil.versionCompare("ab-12-cd-34", "ab-0012-cd-0034");
        Assert.assertTrue(cmp == 0);
        cmp = VersionUtil.versionCompare("ab-12-cd-35", "ab-0012-cd-0034");
        Assert.assertTrue(cmp > 0);
        
        cmp = VersionUtil.versionCompare("ab-0012-cd-0034", "ab-12-cd-34");
        Assert.assertTrue(cmp == 0);
        cmp = VersionUtil.versionCompare("ab-0012-cd-0034", "ab-12-cd-35");
        Assert.assertTrue(cmp < 0);
        
        cmp = VersionUtil.versionCompare("ab-2", "ab-10");
        Assert.assertTrue(cmp < 0);
        cmp = VersionUtil.versionCompare("ab-10", "ab-2");
        Assert.assertTrue(cmp > 0);
        
        cmp = VersionUtil.versionCompare("10a", "2a");
        Assert.assertTrue(cmp > 0);
        cmp = VersionUtil.versionCompare("2a", "10a");
        Assert.assertTrue(cmp < 0);

        cmp = VersionUtil.versionCompare("ab", "a1");
        Assert.assertTrue(cmp > 0);
        cmp = VersionUtil.versionCompare("a1", "ab");
        Assert.assertTrue(cmp < 0); 

        cmp = VersionUtil.versionCompare("ab", "ab1");
        Assert.assertTrue(cmp < 0);
        cmp = VersionUtil.versionCompare("ab1", "ab");
        Assert.assertTrue(cmp > 0);
        
        cmp = VersionUtil.versionCompare("123abc", "0123abc");
        Assert.assertTrue(cmp == 0);
        cmp = VersionUtil.versionCompare("0123abc", "123abc");
        Assert.assertTrue(cmp == 0);
    }

}
