package io.dj.pagination;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <h1>
 * Pagination 어노테이션은 Mybatis @Mapper 어노테이션 기반으로 구현되었으며,<br>
 * SQL 쿼리문의 페이징 처리 및 전체 데이터 갯수 조회 기능을 제공합니다.<br>
 * 
 * </h1>
 * <p>
 *  Pagination 어노테이션을 사용하여 사용자가 작성한 SQL문에 DB에 맞는 페이징 기능을 적용할 수 있습니다.<br>
 *  DBMS정보를 <strong>application.properties</strong>에 정의해주세요.<br>
 *  * 지원하는 DBMS 정보 : mariadb mysql oracle postgresql sqlserver mssql<br><br>
 *    <strong>ex) paging.dbms = mssql</strong><br>
 * </p>
 * <hr>
 * @props 
 * <strong>selectSQL</strong> : 각 Database에 맞게 사용자가 작성한 SQL문을 페이징 처리할 SQL문입니다.(<strong>default: DBMS별 PAGINATION SQL</strong>) <br>
 * <ul>
 *  <strong>* 오라클의 경우 12c버전부터 적용 가능합니다.</strong>
 * <li>postgresql : %s OFFSET %d LIMIT %d</li>
 * <li>sqlserver(mssql) : %s OFFSET %d ROWS FETCH NEXT %d ROWS ONLY</li>
 * <li>oracle : %s OFFSET %d ROWS FETCH NEXT %d ROWS ONLY</li>
 * <li>mysql : %s LIMIT %d, %d</li>
 * <li>mariadb: %s LIMIT %d, %d</li>
 * </ul>
 * <strong>countSQL</strong> :  사용자가 작성한 SQL문의 데이터 전체 갯수를 조회하는 SQL문입니다. (<strong>default: SELECT COUNT(1) FROM (%s) K</strong>)<br>
 * <strong>pageCount</strong> :  한 페이지에 보여질 데이터 갯수입니다. (<strong>default: 10</strong>)<br>
 * <strong>mode</strong> : 실행할 SQL Mode 입니다. (<strong>default: PageMode.PAGINATION</strong>)<br>
 * <ul>
 * <li>PageMode.PURE_DATA   : 사용자가 직접 작성한 SQL문을 페이징 처리 없이 그대로 실행합니다.</li>
 * <li>PageMode.PAGINATION  : 사용자가 작성한 SQL문을 기반으로 페이징을 실행합니다.</li>
 * <li>PageMode.TOTAL_COUNT : 사용자가 작성한 SQL문을 기반으로 데이터 전체 갯수만 실행합니다.</li>
 * </ul>
 * <strong>mapperId</strong> : mapper에서 실행될 select id를 입력합니다. (<strong>default: 사용자가 작성한 메소드명</strong>)<br>
 * @@return PaginationResult&lt;T&gt
 * @@throws PagingException 다음의 경우 발생:
 * <ul>
 * <li>필수 매개변수인 <strong>pageNum</strong>이 누락된 경우</li>
 * <li>요청한 페이지 번호가 마지막 페이지보다 큰 경우</li>
 * <li>SQL문 실행 중 오류가 발생한 경우</li>
 * <li>지원하지 않는 DBMS이거나, application.properties에 dbms정보가 누락된 경우</li>
 * </ul>
 * @author DJ Lee
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Pagination {	
	
	/**
	 * PURE_DATA : SQL 쿼리문을 그대로 조회합니다.<br>
	 * PAGINATION : 페이징 처리 및 전체 데이터 갯수를 조회합니다.<br>
	 * TOTAL_COUNT : SQL 실행문의 전체 데이터 갯수를 조회합니다.
	 */
	enum PageMode {
		PURE_DATA,
		PAGINATION,
		TOTAL_COUNT
	}
	String selectSQL() default "";
	String countSQL() default "SELECT COUNT(1) FROM (%s) K";
	int pageCount() default 10;
	PageMode mode() default PageMode.PAGINATION;
	String mapperId() default "";
}
