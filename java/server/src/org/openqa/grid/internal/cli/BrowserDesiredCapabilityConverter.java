// Licensed to the Software Freedom Conservancy (SFC) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The SFC licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.openqa.grid.internal.cli;

import static org.openqa.selenium.json.Json.MAP_TYPE;

import com.beust.jcommander.IStringConverter;

import java.util.Map;

import org.openqa.selenium.json.Json;
import org.openqa.selenium.json.JsonException;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

class BrowserDesiredCapabilityConverter implements IStringConverter<DesiredCapabilities> {
  // Allow pass-thru of valid json data as the value in the key=value list.  Backward
  // compatible with the previous literal key=value list processor.  If an option value
  // containing json-like content successfully evaluates to json input, the resulting
  // output map is stored as the option value result.

  @SuppressWarnings("unchecked")
  @Override
  public DesiredCapabilities convert( String input ) {
    DesiredCapabilities capabilities = new DesiredCapabilities();

    int index = 0;

    if ( ( null != input ) && ( index < input.length() ) ) {
      do {
        // discard whitespace preceding key/identifier
        while ( Character.isWhitespace( input.charAt( index ) ) )
        {
          index++;

          if ( index == input.length() ) {
            return capabilities;
          }
        }

        int idStart = index;
        int idStop = idStart;

        String name = null;

        switch ( input.charAt( idStart ) ) {
          // if any productions (other than literal) were allowed for key/identifier field in the
          // DesiredCapabilities list, they would go here

          default: {
            idStop = acceptLiteralIdentifier( idStart, input );

            name = input.substring( idStart, idStop );

            name = name.trim();

            index = idStop;
          }
            break;
        }

        if ( ( index < input.length() ) && ( ',' != input.charAt( index ) ) ) {
          // index at '=' or at end of input
          index++;
        }

        String value = null;

        if ( index < input.length() ) {
          // discard whitespace preceding definition
          while ( Character.isWhitespace( input.charAt( index ) ) ) {
            index++;

            if ( index == input.length() ) {
              if ( null != name ) {
                capabilities.setCapability( name, "" );
              }

              return capabilities;
            }
          }

          int defnStart = index;
          int defnStop = defnStart;

          switch( input.charAt( defnStart ) ) {
            case '"': {
              defnStop = acceptDblQuotedString( defnStart, input );

              value = ( defnStop < input.length() ) ? input.substring( defnStart, defnStop + 1 ) : input.substring( defnStart );

              value = value.trim();

              index = 1 + defnStop;

              while ( index < input.length() ) {
                if ( Character.isWhitespace( input.charAt( index ) ) ) {
                  index++;
                }
                else {
                  break;
                }
              }

              if ( ( index < input.length() ) && ( ',' != input.charAt( index ) ) ) {
                int trailingStart = index;

                System.err.println( "ERROR: Found extraneous trailing symbols after " + name + " definition value at character "
                                    + trailingStart + " that were discarded." );

                while ( index < input.length() ) {
                  if ( ',' != input.charAt( index ) ) {
                    index++;
                  }
                  else {
                    break;
                  }
                }

                System.err.println( "   discarded (" + input.substring( trailingStart, index ) + ") symbols" );
              }
            }
              break;

            case '[': {
              defnStop = acceptJsonArrayDefinition( defnStart, input );

              value = ( defnStop < input.length() ) ? input.substring( defnStart, defnStop + 1 ) : input.substring( defnStart );

              value = value.trim();

              index = 1 + defnStop;

              while ( index < input.length() ) {
                if ( Character.isWhitespace( input.charAt( index ) ) ) {
                  index++;
                }
                else {
                  break;
                }
              }

              if ( ( index < input.length() ) && ( ',' != input.charAt( index ) ) ) {
                int trailingStart = index;

                System.err.println( "ERROR: Found extraneous trailing symbols after " + name + " definition value at character "
                                    + trailingStart + " that were discarded." );

                while ( index < input.length() ) {
                  if ( ',' != input.charAt( index ) ) {
                    index++;
                  }
                  else {
                    break;
                  }
                }

                System.err.println( "   discarded (" + input.substring( trailingStart, index ) + ") symbols" );
              }

            }
              break;

            case '{': {
              defnStop = acceptJsonObjectDefinition( defnStart, input );

              value = ( defnStop < input.length() ) ? input.substring( defnStart, defnStop + 1 ) : input.substring( defnStart );

              value = value.trim();

              index = 1 + defnStop;

              while ( index < input.length() ) {
                if ( Character.isWhitespace( input.charAt( index ) ) ) {
                  index++;
                }
                else {
                  break;
                }
              }

              if ( ( index < input.length() ) && ( ',' != input.charAt( index ) ) ) {
                int trailingStart = index;

                System.err.println( "ERROR: Found extraneous trailing symbols after " + name + " definition value at character "
                                    + trailingStart + " that were discarded." );

                while ( index < input.length() ) {
                  if ( ',' != input.charAt( index ) ) {
                    index++;
                  }
                  else {
                    break;
                  }
                }

                System.err.println( "   discarded (" + input.substring( trailingStart, index ) + ") symbols" );
              }
            }
              break;

            default: {
              defnStop = acceptLiteralDefinition( defnStart, input );

              value = input.substring( defnStart, defnStop );

              value = value.trim();

              index = defnStop;
            }
          }

          // index at ',' or at end of input
          index++;

          if ( value.startsWith( "\"" ) ) {
            if ( ( value.length() > 1 ) && ( value.endsWith( "\"" ) ) ) {
              value = value.substring( 1, value.length() - 1 );
            }
            else {
              value = value.substring( 1 );
            }
          }

          if ( ( name.length() > 0 ) || ( value.length() > 0 ) ) {
            capabilities.setCapability( name, value );

            if ( CapabilityType.VERSION.equals( name ) ) {
              // store version as a string, DesiredCapabilities assumes version is a string
              capabilities.setCapability( name, value );
            }
            else {
              try {
                final Long x = Long.parseLong( value );

                capabilities.setCapability( name, x );
              }
              catch ( NumberFormatException nfe ) {
                // ignore the exception. process as boolean or string.
                if ( "true".equals( value ) || "false".equals( value ) ) {
                  capabilities.setCapability( name, Boolean.parseBoolean( value ) );
                }
                else {
                  // promote any options value containing valid json to Map< String, Object >
                  final String suffixStr = "options";

                  final int suffixLen = suffixStr.length();

                  // if name endsWith 'options' and value is a well-formed json object
                  if ( ( name.length() > 0 ) && ( name.regionMatches( true, name.length() - suffixLen, suffixStr, 0, suffixLen ) )
                       && ( value.length() > 0 ) ) {
                     Map< String, Object > options = null;

                     try {
                        options = new Json().toType( value, MAP_TYPE );

                        capabilities.setCapability( name, options );
                     }
                     catch( JsonException je ) {
                        capabilities.setCapability( name, value );
                     }
                  }
                  else {
                     capabilities.setCapability( name, value );
                  }
                }
              }
            }
          }
        }
        else if ( name.length() > 0 ) {
          capabilities.setCapability( name, "" );

          return capabilities;
        }
      }
      while ( index < input.length() );
    }

    return capabilities;
  }

  private int acceptDblQuotedString( int idStart, String input ) {
    if ( '"' == input.charAt( idStart ) ) {
      int index = idStart + 1;

      while ( index < input.length() ) {
        switch ( input.charAt( index ) ) {
          case '"': return index;

          case '\\': index += 2;
            break;

          default: index++;
        }
      }

      return index;
    }

    return -1;
  }

  private int acceptLiteralIdentifier( int idStart, String input ) {
    int index = idStart;

    while ( ( index < input.length() ) && ( ',' != input.charAt( index ) ) && ( '=' != input.charAt( index ) ) ) {
      index++;
    }

    return index;
  }

  private int acceptLiteralDefinition( int defnStart, String input ) {
    int index = defnStart;

    while ( ( index < input.length() ) && ( ',' != input.charAt( index ) ) ) {
      index++;
    }

    return index;
  }

  private int acceptJsonArrayDefinition( int defnStart, String input ) {
    if ( '[' == input.charAt( defnStart ) ) {
      int index = defnStart + 1;

      while ( index < input.length() ) {
        switch ( input.charAt( index ) ) {
          case ']': return index;

          case '[': index = 1 + acceptJsonArrayDefinition( index, input );
            break;

          case '{': index = 1 + acceptJsonObjectDefinition( index, input );
            break;

          case '"': index = 1 + acceptDblQuotedString( index, input );
            break;

          default: index++;
        }
      }

      return index;
    }

    return -1;
  }

  private int acceptJsonObjectDefinition( int defnStart, String input ) {
    if ( '{' == input.charAt( defnStart ) ) {
      int index = defnStart + 1;

      while ( index < input.length() ) {
        switch ( input.charAt( index ) ) {
          case '}': return index;

          case '{': index = 1 + acceptJsonObjectDefinition( index, input );
            break;

          case '[': index = 1 + acceptJsonArrayDefinition( index, input );
            break;

          case '"': index = 1 + acceptDblQuotedString( index, input );
            break;

          default: index++;
        }
      }

      return index;
    }

    return -1;
  }
}
