/*
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hasor.dataql.fx.db.dialect;
import net.hasor.dataql.fx.db.parser.FxSql;

import java.util.List;
import java.util.Map;

/**
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2020-04-08
 */
public class OracleDialect extends AbstractDialect {
    @Override
    public BoundSql getPageSql(FxSql fxSql, Map<String, Object> paramMap, int start, int limit) {
        String buildSqlString = fxSql.buildSqlString(paramMap);
        List<Object> paramArrays = fxSql.buildParameterSource(paramMap);
        //
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT * FROM ( ");
        sqlBuilder.append(" SELECT TMP_PAGE.*, ROWNUM ROW_ID FROM ( ");
        sqlBuilder.append(buildSqlString);
        sqlBuilder.append(" ) TMP_PAGE)");
        sqlBuilder.append(" WHERE ROW_ID > ? AND ROW_ID <= ?");
        paramArrays.add(start);
        paramArrays.add(start + limit);
        //
        buildSqlString = sqlBuilder.toString();
        return new BoundSql(buildSqlString, paramArrays.toArray());
    }
}