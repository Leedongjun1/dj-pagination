package io.dj.pagination;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.SqlSession; // MyBatis SqlSession
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.stereotype.Component;

import io.dj.pagination.Pagination.PageMode;
import io.dj.pagination.exception.PagingErrorCode;
import io.dj.pagination.exception.PagingException;
import io.dj.pagination.vo.PaginationResult;

@Aspect
@Component
@SuppressWarnings("unchecked")
public class PaginationAspect {
	@Autowired
	private SqlSession sqlSession;

	@Autowired
	private DataSource datasource;
	
	@Value("${paging.dbms}")
    private String dbmsVersion;
	
	private PaginationSQL dbmsSelectSql;
	
	@Around("@annotation(pagination)")
	private <T> PaginationResult<T> applyPagination(ProceedingJoinPoint joinPoint, Pagination pagination) {
		if (dbmsSelectSql == null) {
			String dbmsInfo = "(지원하는 DBMS 정보 :";
	    	for (PaginationSQL sql : PaginationSQL.values()) {
	    		dbmsInfo+=" " +sql.getDbms();
	            if (sql.getDbms().equalsIgnoreCase(dbmsVersion.toLowerCase().trim())) {
	            	dbmsSelectSql = sql;
	            }
	        }

			if (dbmsSelectSql == null) {
				throw new PagingException(PagingErrorCode.UNSUPPORTED_DBMS,dbmsInfo+")");
			}
		}
		String paginationSql = pagination.selectSQL().equals("")? dbmsSelectSql.getSql() : pagination.selectSQL();
		// 매개변수 정보 추출
		Map<String, Object> param = getParameter(joinPoint);
		// return 타입 추출
		Class<T> resultType = getReturnType(joinPoint);
		// mapper에 지정된 SQL 조회
		String originalQuery = getMyBatisQuery(joinPoint,pagination, param);
		List<T> data = null;
		int totalCount = 0; 
	    int lastPage = 0;
		String countQuery = String.format(pagination.countSQL(), originalQuery);
		if (pagination.mode() == PageMode.PAGINATION) {
			// pageNum 필수 파라미터
			int pageNum = (int) param.getOrDefault("pageNum", 0);
			if (pageNum < 1) {
				// 필수 매개변수 pageNum 이 없는 경우,
				throw new PagingException(PagingErrorCode.NOT_FOUND_REQUIRED_PARAMETER," (pageNum이 1보다 작거나, 누락되었습니다.)");
			}
			// 웹에서 pageCount를 지정하여, 조회하는 경우 (선택)
			int pageCount = (int)param.getOrDefault("pageCount", 0)>0? (int)param.get("pageCount"):(int) pagination.pageCount();
			totalCount = executeTotalCnt(countQuery);
			lastPage = (int)Math.ceil( (double)totalCount / (double)pageCount);
			
			if (pageNum > lastPage) {
				// 조회하고자 하는 페이지가 마지막 페이지 정보보다 높은 경우
				throw new PagingException(PagingErrorCode.PAGING_ERROR, " (실행 페이지:"+pageNum+", 마지막 페이지:"+lastPage+")");
			}
			int offset = (pageNum - 1) * pageCount;

			String paginatedQuery = String.format(paginationSql, originalQuery, offset, pageCount);
			data = executeSql(paginatedQuery, resultType);
		} else if (pagination.mode() == PageMode.PURE_DATA) {
			data = executeSql(originalQuery, resultType);
		} else if (pagination.mode() == PageMode.TOTAL_COUNT) {
			totalCount = executeTotalCnt(countQuery);
		}

		return new PaginationResult<>(data, totalCount, lastPage);
	}
	/**
	 * 매개변수 정보
	 * @param joinPoint
	 * @return
	 */
	private Map<String, Object> getParameter(ProceedingJoinPoint joinPoint){
		Object[] args = joinPoint.getArgs();
		Map<String, Object> param = new HashMap<String, Object>();
		// 매개변수 형태가 Map이 아닌 경우, Map으로 변환
		if (args.length == 0 || !(args[0] instanceof Map)) {
			Class<?> clazz = args[0].getClass();
			for (Field field : clazz.getDeclaredFields()) {
				field.setAccessible(true);
				try {
					param.put(field.getName(), field.get(args[0]));
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		} else {
			param = (Map<String, Object>) args[0];
		}

		return param;
	}
	/**
	 * 실행하는 method의 return type 정보
	 * @param <T>
	 * @param joinPoint
	 * @return
	 */
	private <T> Class<T> getReturnType(ProceedingJoinPoint joinPoint){
		// MethodSignature를 사용하여 메서드 반환 타입 추출
	    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
	    Method method = signature.getMethod();
	    Class<T> resultType = null;
	    ParameterizedType returnType = (ParameterizedType) method.getGenericReturnType();

	    // ReturnType 분류
	    if (returnType.getActualTypeArguments()[0] instanceof ParameterizedType) {
	        ParameterizedType paramType = (ParameterizedType) returnType.getActualTypeArguments()[0];
	        if (paramType.getRawType() == Map.class) {
	            resultType = (Class<T>) Map.class; // Map 타입으로 처리
	        } 
	    } else if (returnType.getActualTypeArguments()[0] instanceof Class) {
	        resultType = (Class<T>) returnType.getActualTypeArguments()[0]; // 일반 클래스 타입
	    }
	    
	    return resultType;
	}
	/**
	 * 실행하는 dao의 mybatis 쿼리 
	 * @param joinPoint
	 * @param pagination
	 * @param param
	 * @return
	 */
	private String getMyBatisQuery(ProceedingJoinPoint joinPoint,Pagination pagination, Map<String, Object> param) {
		
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();
		String targetMethod = pagination.mapperId().equals("")?method.getName():pagination.mapperId();
		
		BoundSql boundSql = sqlSession.getConfiguration().getMappedStatement(targetMethod)
				.getBoundSql(joinPoint.getArgs()[0]);
		String sql = boundSql.getSql();

		for (ParameterMapping bindingParam : boundSql.getParameterMappings()) {
			sql = sql.replaceFirst("\\?", "'" + param.get(bindingParam.getProperty()).toString() + "'");
		}
		return sql;
	}
	/**
	 * return type이 Map인 경우 
	 * 
	 * @param <T>
	 * @param resultSet
	 * @return
	 * @throws Exception
	 */
	private <T> List<T> resultMap(ResultSet resultSet) throws Exception{
		List<T> list = new ArrayList<T>();
		ResultSetMetaData metaData = resultSet.getMetaData();
		int columnCount = metaData.getColumnCount();
		
		while (resultSet.next()) {
			Map<String, Object> row = new HashMap<>();
			for (int i = 1; i <= columnCount; i++) {
				String columnName = metaData.getColumnName(i);
				Object value = resultSet.getObject(i);
				row.put(columnName, value);
			}
			list.add((T) row);
		}
		
		return list;
	}
	/**
	 * return type이 vo인 경우
	 * @param <T>
	 * @param resultSet
	 * @param clazz
	 * @return
	 * @throws Exception
	 */
	private <T> List<T> resultVo(ResultSet resultSet, Class<T> clazz) throws Exception{
		List<T> list = new ArrayList<T>();
		ResultSetMetaData metaData = resultSet.getMetaData();
		int columnCount = metaData.getColumnCount();
		
		while (resultSet.next()) {
			T entity = clazz.getDeclaredConstructor().newInstance();
			for (int i = 1; i <= columnCount; i++) {
				String columnName = JdbcUtils.convertUnderscoreNameToPropertyName(metaData.getColumnName(i));
				Field field = clazz.getDeclaredField(columnName);
				field.setAccessible(true);
				Object value = resultSet.getObject(i);
				field.set(entity, value);
			}
			list.add(entity);
		}
		
		return list;
	}
	/**
	 * 전체 카운트 정보 실행
	 * @param sql
	 * @return
	 * @throws SQLException
	 */
	private int executeTotalCnt(String sql) {
		int totalCnt = 0;
		try {
			Connection con = datasource.getConnection();
			PreparedStatement pstmt = con.prepareStatement(sql);
			ResultSet resultSet = pstmt.executeQuery();
			
			if (resultSet.next()) {
				totalCnt = resultSet.getInt(1);
			}
		} catch (SQLSyntaxErrorException e) {
			throw new PagingException(PagingErrorCode.SQL_SYNTAX_ERROR,"\n"+sql);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return totalCnt;
	}
	/**
	 * sql execute
	 * @param <T>
	 * @param sql
	 * @param clazz
	 * @return
	 * @throws SQLException
	 */
	private <T> List<T> executeSql(String sql, Class<?> clazz) {
		try {
			Connection con = datasource.getConnection();
			PreparedStatement pstmt = con.prepareStatement(sql);
			ResultSet resultSet = pstmt.executeQuery();
			if (Map.class.isAssignableFrom(clazz)) {
				return resultMap(resultSet);
			} else {
				return (List<T>)resultVo(resultSet, clazz);
			}
		} catch (SQLSyntaxErrorException e) {
			throw new PagingException(PagingErrorCode.SQL_SYNTAX_ERROR,"\n"+sql);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
