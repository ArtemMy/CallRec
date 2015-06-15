/**
 This file is part of CallRec.

 CallRec is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 CallRec is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with CallRec.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.artem.callrec;

import android.util.Log;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Created by 805268 on 18.05.2015.
 */
public class ResponseHandler extends DefaultHandler {
    private static String FILE_NAME, out_str;
    private static int i = 0, j = 0;

    ResponseHandler(String filename, int n)
    {
        FILE_NAME = filename;
        i = n;
        j = 0;
        out_str = "";
    }
    @Override
    public void startDocument() throws SAXException {
        Log.e("request", "start document   : ");
    }

    @Override
    public void endDocument() throws SAXException {
        try{
            i = 0;
            j = 0;
            File f = new File(FILE_NAME);
            if(!f.exists()) {
                PrintStream out = new PrintStream(f);
                out.println(out_str);
            } else {
                PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f.getAbsolutePath(), true)));
                out.println(out_str);
                out.close();
            }
        }catch (IOException e) {
            Log.e("service", Log.getStackTraceString(e));
        }
        Log.e("request", "end document     : ");
    }

    @Override
    public void startElement(String uri, String localName,
                             String qName, Attributes attributes)
            throws SAXException {

        Log.e("request", "start element    : " + qName);
        Log.e("request", "attributes    : " + attributes.getValue(0));
    }
    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        Log.e("request", "end element      : " + qName);
    }
    @Override
    public void characters(char ch[], int start, int length)
            throws SAXException {
        String res = new String(ch, start, length);
        if(res.replaceAll("\\s","").isEmpty())
            return;
        out_str += ("#" + i + "." + j + "\n");
        j++;
        out_str += res + "\n";
    }

/*    public class Response
    {
        public int c;
        String response;
        public void SetC (int _c ) { c = _c; }
        public void setString(String _s){response = _s;}
    }
    //This is the list which shall be populated while parsing the XML.
    private ArrayList responseList = new ArrayList();

    //As we read any XML element we will push that in this stack
    private Stack responseStack = new Stack();

    //As we complete one user block in XML, we will push the User instance in responseList
    private Stack<Response> objectStack = new Stack();

    public void startDocument() throws SAXException {
        //System.out.println("start of the document   : ");
    }

    public void endDocument() throws SAXException {
        //System.out.println("end of the document document     : ");
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        //Push it in element stack
        this.responseStack.push(qName);

        //If this is start of 'user' element then prepare a new User instance and push it in object stack
        if ("variant".equals(qName)) {
            //New User instance
            Response variant  = new Response();

            //Set all required attributes in any XML element here itself
            if (attributes != null && attributes.getLength() == 1)
            {
                variant.SetC(Integer.parseInt(attributes.getValue(0)));
            }
            this.objectStack.push(variant);
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        //Remove last added  element
        this.responseStack.pop();

        //User instance has been constructed so pop it from object stack and push in responseList
        if ("variant".equals(qName)) {
            Response object = this.objectStack.pop();
            this.responseList.add(object);
        }
    }


    // This will be called everytime parser encounter a value node

    public void characters(char[] ch, int start, int length) throws SAXException {
        String value = new String(ch, start, length).trim();

        if (value.length() == 0) {
            return; // ignore white space
        }

        //handle the value based on to which element it belongs
        Response response = (Response) this.objectStack.peek();
        response.setString(value);
    }


    // Utility method for getting the current element in processing
     x
    private String currentElement() {
        return ((Response)this.responseStack.peek()).response;
    }

    //Accessor for responseList object
    public ArrayList getUsers() {
        return responseList;
    }*/
}