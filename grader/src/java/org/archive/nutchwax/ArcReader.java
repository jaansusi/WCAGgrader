/*
 * Copyright (C) 2008 Internet Archive.
 * 
 * This file is part of the archive-access tools project
 * (http://sourceforge.net/projects/archive-access).
 * 
 * The archive-access tools are free software; you can redistribute them and/or
 * modify them under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or any
 * later version.
 * 
 * The archive-access tools are distributed in the hope that they will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser
 * Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License along with
 * the archive-access tools; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.archive.nutchwax;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveReaderFactory;
import org.archive.io.ArchiveRecord;
import org.archive.io.ArchiveRecordHeader;

import org.archive.io.arc.ARCConstants;
import org.archive.io.arc.ARCReader;
import org.archive.io.arc.ARCRecord;
import org.archive.io.arc.ARCRecordMetaData;
import org.archive.io.warc.WARCConstants;
import org.archive.io.warc.WARCRecord;

import org.apache.commons.httpclient.Header;

/**
 * <p>
 *   Reader of both ARC and WARC format archive files.  This is not a
 *   general-purpose archive file reader, but is written specifically
 *   for NutchWAX.  It's possible that this could become a
 *   general-purpose archive file reader, but for now, consider it
 *   custom-tailored to the needs of NutchWAX.
 * </p>
 * <p>
 *   <code>ArcReader</code> is a wrapper around the underlying
 *   <code>ArchiveReader</code> implementation
 *   (<code>ARCReader</code>/<code>WARCReader</code>) which converts
 *   <code>WARCRecord</code>s to <code>ARCRecord</code>s on the fly.
 * </p>
 * <p>
 *   If an <code>ARCReader</code> is being wrapped, then the
 *   underlying <code>ARCRecord</code>s are read and passed-through
 *   unmolested.
 * </p>
 * <p>
 *   If a <code>WARCReader</code> is being wrapped, then the
 *   <code>WARCRecord</code>s are converted to <code>ARCRecord</code>s
 *   on the fly.
 * </p>
 * <p>
 *   <strong>WARNING:</strong> We only convert WARC
 *   <code>response</code> records.  All other WARC record types are
 *   returned as <code>null</code> by the iterator's
 *   <code>next()</code> method.  So, when using the iterator, don't
 *   forget to check for a <code>null</code> value returned by
 *   <code>next()</code>.
 * </p>
 */
public class ArcReader implements Iterable<ARCRecord>
{
  private ArchiveReader reader;

  /**
   * Construct an <code>ArcReader<code> wrapping an
   * <code>ArchiveReader</code> instance.
   *
   * @param reader the ArchiveReader instance to wrap
   */
  public ArcReader( ArchiveReader reader )
  {
    this.reader = reader;
  }

  /**
   * Returns an iterator over <code>ARCRecord</code>s in the wrapped
   * <code>ArchiveReader</code>, converting <code>WARCRecords</code>
   * to <code>ARCRecords</code> on-the-fly.
   *
   * @return an interator
   */
  public Iterator<ARCRecord> iterator( )
  {
    return new ArcIterator( );
  }

  /**
   * 
   */
  private class ArcIterator implements Iterator<ARCRecord>
  {
    private Iterator<ArchiveRecord> i;

    /**
     * Construct an <code>ArcIterator</code>, skipping the header
     * record if the wrapped reader is an <code>ARCReader</code>.
     */
    public ArcIterator( )
    {
      this.i = ArcReader.this.reader.iterator( );
      
      if ( ArcReader.this.reader instanceof ARCReader )
        {
          // Skip the first record, which is a "filedesc://"
          // record describing the ARC file.
          if ( this.i.hasNext( ) ) this.i.next( );
        }
    }

    /**
     * Returns <code>true</code> if the iteration has more elements.
     * Will return <code>true</code> even if the value returned by the
     * next call to <code>next()</code> returns <code>null</code>.
     *
     * @return <code>true</code> if the iterator has more elements.
     */
    public boolean hasNext( )
    {
      return this.i.hasNext( );
    }
    
    /**
     * Returns the next element in the iteration. Calling this method
     * repeatedly until the <code>hasNext()</code> method returns
     * <code>false</code> will return each element in the underlying
     * collection exactly once.
     * 
     * @return the next element in the iteration, which can be <code>null</code>
     */
    public ARCRecord next( )
    {
      try
        {
          ArchiveRecord record = this.i.next( );
          
          if ( record instanceof ARCRecord )
            {
              // Just return the ARCRecord as-is.
              ARCRecord arc = (ARCRecord) record;
              
              return arc;
            }
          
          if ( record instanceof WARCRecord )
            {
              WARCRecord warc = (WARCRecord) record;
              
              ARCRecord arc = convert( warc );

              return arc;
            }

          // If we get here then the record we reaad in was neither an ARC
          // or WARC record.  What is a good exception to throw?
          throw new RuntimeException( "Record neither ARC nor WARC: " + record.getClass( ) );
        }
      catch ( IOException ioe )
        {
          throw new RuntimeException( ioe );
        }
    }

    /**
     * Unsupported optional operation.
     *
     * @throw UnsupportedOperationException
     */
    public void remove( )
    {
      throw new UnsupportedOperationException( );
    }

    /**
     * Convert a WARCRecord to an ARCRecord.  Only "response"
     * WARCRecords are converted to meaningful ARCRecords.  All other
     * WARCRecord types are converted to <code>null</code>.
     *
     * @param warc the WARCRecord to convert
     * @return the corresponding ARCRecord, <code>null</code> if WARCRecord not a "reponse" record
     */
    private ARCRecord convert( WARCRecord warc )
      throws IOException
    {
      ArchiveRecordHeader header = warc.getHeader( );
      
      // We only care about "response" WARC records.
      if ( ! WARCConstants.RESPONSE.equals( header.getHeaderValue( WARCConstants.HEADER_KEY_TYPE ) ) )
        {
          return null;
        }
              
      // Construct an ARCRecordMetadata object based on the info in
      // the ArchiveRecordHeader.
      Map arcMetadataFields = new HashMap( );
      arcMetadataFields.put( ARCConstants.URL_FIELD_KEY,       header.getHeaderValue( WARCConstants.HEADER_KEY_URI  ) );
      arcMetadataFields.put( ARCConstants.IP_HEADER_FIELD_KEY, header.getHeaderValue( WARCConstants.HEADER_KEY_IP   ) );
      arcMetadataFields.put( ARCConstants.MIMETYPE_FIELD_KEY,  header.getHeaderValue( null ) );  // We don't know the MIME type of the *payload* in a WARC (yet)
      arcMetadataFields.put( ARCConstants.LENGTH_FIELD_KEY,    header.getHeaderValue( WARCConstants.CONTENT_LENGTH  ) );
      arcMetadataFields.put( ARCConstants.VERSION_FIELD_KEY,   header.getHeaderValue( null ) );  // FIXME: Do we need actual values for these?
      arcMetadataFields.put( ARCConstants.ABSOLUTE_OFFSET_KEY, header.getOffset( ) );

      // Dates must be converted from WARC format to 14-digit format,
      // that is, from YYYY-MM-DDTHH:MM:SSZ to YYYYMMDDHHMMSS
      String warcDate = (String) header.getHeaderValue( WARCConstants.HEADER_KEY_DATE );
      StringBuilder date = new StringBuilder( )
        .append( warcDate,  0, 4  )
        .append( warcDate,  5, 7  )
        .append( warcDate,  8, 10 )
        .append( warcDate, 11, 13 )
        .append( warcDate, 14, 16 )
        .append( warcDate, 17, 19 );

      arcMetadataFields.put( ARCConstants.DATE_FIELD_KEY, date.toString( ) );
              
      ARCRecordMetaData metadata = new ARCRecordMetaData( header.getReaderIdentifier( ), arcMetadataFields );

      metadata.setDigest( (String) header.getHeaderValue( WARCConstants.HEADER_KEY_PAYLOAD_DIGEST ) );
              
      // Then, create an ARCRecord using the WARCRecord and the
      // ARCRecordMetaData object we just created.
      ARCRecord arc = new ARCRecord( warc, 
                                     metadata,
                                     0,  // offset
                                     ArcReader.this.reader.isDigest( ),
                                     ArcReader.this.reader.isStrict( ),
                                     true  // parse HTTP headers
                                   );
      
      // Now that we've created the ARCRecord, we get the HTTP headers
      // from it.  From these HTTP headers, we obtain the Content-Type
      // of the ARCRecord's payload, then set value as the MIME-type
      // of the ARCRecord itself.
      
      // If the response is something other than HTTP
      // (like DNS) there are no HTTP headers.  
      if ( arc.getHttpHeaders( ) != null )
        {
          for ( Header h : arc.getHttpHeaders( ) )
            {
              if ( h.getName( ).equals( "Content-Type" ) )
                {
                  arc.getMetaData( ).getHeaderFields( ).put( ARCConstants.MIMETYPE_FIELD_KEY, h.getValue( ) );
                }
            }
        }
      
      return arc;
    }

  }


  /**
   * Simple test/debug driver to read an archive file and print out
   * the header for each record.
   */
  public static void main( String args[] ) throws Exception
  {
    if ( args.length != 1 )
      {
        System.out.println( "ArcReader <(w)arc file>" );
        System.exit( 1 );
      }

    String arcName = args[0];

    ArchiveReader r = ArchiveReaderFactory.get( arcName );
    r.setDigest( true );

    ArcReader reader = new ArcReader( r );

    for ( ARCRecord rec : reader )
      {
        // Must call close() for digest calculation to be finished.
        rec.close( );

        if ( rec != null ) System.out.println( rec.getHeader( ) );
      }
  }
}
