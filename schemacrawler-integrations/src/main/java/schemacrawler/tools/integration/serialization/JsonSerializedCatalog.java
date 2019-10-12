/*
========================================================================
SchemaCrawler
http://www.schemacrawler.com
Copyright (c) 2000-2019, Sualeh Fatehi <sualeh@hotmail.com>.
All rights reserved.
------------------------------------------------------------------------

SchemaCrawler is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

SchemaCrawler and the accompanying materials are made available under
the terms of the Eclipse Public License v1.0, GNU General Public License
v3 or GNU Lesser General Public License v3.

You may elect to redistribute this code under any of these licenses.

The Eclipse Public License is available at:
http://www.eclipse.org/legal/epl-v10.html

The GNU General Public License v3 and the GNU Lesser General Public
License v3 are available at:
http://www.gnu.org/licenses/

========================================================================
*/
package schemacrawler.tools.integration.serialization;


import static com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY;
import static com.fasterxml.jackson.databind.SerializationFeature.*;
import static java.util.Objects.requireNonNull;

import java.io.OutputStream;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.introspect.ObjectIdInfo;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import schemacrawler.schema.Catalog;
import schemacrawler.schema.NamedObject;
import schemacrawler.schemacrawler.BaseCatalogDecorator;
import schemacrawler.schemacrawler.SchemaCrawlerException;

/**
 * Decorates a database to allow for serialization to and from plain
 * Java serialization.
 */
public final class JsonSerializedCatalog
  extends BaseCatalogDecorator
  implements SerializableCatalog
{

  private static final long serialVersionUID = 5314326260124511414L;


  private static class ObjectIdGenerator
    extends JacksonAnnotationIntrospector
  {
    @Override
    public ObjectIdInfo findObjectIdInfo(final Annotated ann)
    {
      if (ann.getAnnotated().getClass().isInstance(NamedObject.class))
      {
        return new ObjectIdInfo(PropertyName.construct("@uuid", null),
                                null,
                                ObjectIdGenerators.UUIDGenerator.class,
                                null);
      }
      return super.findObjectIdInfo(ann);
    }
  }

  public JsonSerializedCatalog(final Catalog catalog)
  {
    super(catalog);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final <T> Optional<T> lookupAttribute(final String name)
  {
    return Optional.of(getAttribute(name));
  }

  /**
   * Serializes the database to the writer, as JSON.
   *
   * @param out Output stream to save to
   * @throws SchemaCrawlerException On an exception
   */
  @Override
  public void save(final OutputStream out)
    throws SchemaCrawlerException
  {
    requireNonNull(out, "No output stream provided");
    try
    {
      final FilterProvider filters = new SimpleFilterProvider().addFilter(
        "skip_references_serializer",
        SimpleBeanPropertyFilter.serializeAllExcept("parent",
                                                    "exportedForeignKeys",
                                                    "inportedForeignKeys"));
      @JsonFilter("skip_references_serializer")
      class PropertyFilterMixIn
      {

      }

      final ObjectMapper mapper = new ObjectMapper();
      mapper.enable(ORDER_MAP_ENTRIES_BY_KEYS,
                    INDENT_OUTPUT,
                    USE_EQUALITY_FOR_OBJECT_ID,
                    WRITE_ENUMS_USING_TO_STRING);
      mapper.enable(SORT_PROPERTIES_ALPHABETICALLY);
      mapper.setAnnotationIntrospector(new ObjectIdGenerator());
      mapper.addMixIn(Object.class, PropertyFilterMixIn.class);
      mapper.setFilterProvider(filters);

      // Write JSON to stream
      mapper.writeValue(out, catalog);
    }
    catch (final Exception e)
    {
      throw new SchemaCrawlerException("Could not serialize catalog", e);
    }
  }

}
