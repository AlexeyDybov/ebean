package org.tests.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.tests.model.basic.Customer;
import org.tests.model.basic.Order;
import org.tests.model.basic.ResetBasicData;

import io.ebean.BaseTestCase;
import io.ebean.Ebean;
import io.ebeantest.LoggedSql;

/**
 * Tests if outer joins are correctly used.
 *
 * @author Roland Praml, FOCONIS AG
 *
 */
public class TestOuterJoin extends BaseTestCase {

  @BeforeClass
  public static void setup() {
    ResetBasicData.reset();
  }


  @Test
  public void testOuterOnNullQuery() throws Exception {

    LoggedSql.start();
    List<Customer> list = Ebean.find(Customer.class).where()
        .isNull("orders")
        .le("id", 4) // ignore others than basicData
        .findList();

    assertThat(LoggedSql.collect().get(0))
      .contains(" where not exists "); // perfored with not exists query
    assertThat(list).hasSize(2);

    // use OR construct
    LoggedSql.start();
    list = Ebean.find(Customer.class).where()
        .or()
          .isNull("orders.details.product.name")
          .isNull("orders")
        .endOr()
        .le("id", 4) // ignore others than basicData
        .findList();

    assertThat(LoggedSql.collect().get(0))
      .contains(" left join o_order ")
      .contains(" left join o_order_detail ")
      .contains(" left join o_product ")
      .contains(" or not exists ");

    assertThat(list).hasSize(4);

    // use AND construct
    LoggedSql.start();
    list = Ebean.find(Customer.class).where()
        .and()
          .isNull("orders.details.product.name")
          .isNull("orders")
        .endAnd()
        .le("id", 4) // ignore others than basicData
        .findList();

    assertThat(LoggedSql.collect().get(0))
      .contains(" left join o_order ")
      .contains(" left join o_order_detail ")
      .contains(" left join o_product ")
      .contains(" and not exists ");

    assertThat(list).hasSize(2);

    LoggedSql.start();
    list = Ebean.find(Customer.class).where()
        .isNull("orders.details.product.name")
        .le("id", 4) // ignore others than basicData
        .findList();

    assertThat(LoggedSql.collect().get(0))
      .contains(" left join o_order ")
      .contains(" left join o_order_detail ")
      .contains(" left join o_product ");
    assertThat(list).hasSize(4);
    LoggedSql.stop();
  }

  @Test
  public void testInnerOnNonNullQuery() throws Exception {

    LoggedSql.start();
    List<Customer> list = Ebean.find(Customer.class).where()
        .isNotNull("orders")
        .le("id", 4) // ignore others than basicData
        .findList();

    assertThat(LoggedSql.collect().get(0))
      .contains(" where exists "); // perfored with not exists query
    assertThat(list).hasSize(2);

    // use OR construct
    LoggedSql.start();
    list = Ebean.find(Customer.class).where()
        .or()
          .isNotNull("orders.details.product.name")
          .isNotNull("orders")
        .endOr()
        .le("id", 4) // ignore others than basicData
        .findList();

    assertThat(LoggedSql.collect().get(0))
      .contains(" left join o_order ")
      .contains(" left join o_order_detail ")
      .contains(" left join o_product ")
      .contains(" or exists ");

    assertThat(list).hasSize(2);

    // use AND construct
    LoggedSql.start();
    list = Ebean.find(Customer.class).where()
        .and()
          .isNotNull("orders.details.product.name")
          .isNotNull("orders")
        .endAnd()
        .le("id", 4) // ignore others than basicData
        .findList();

    assertThat(LoggedSql.collect().get(0))
    .doesNotContain(" left join ")
      .contains(" and exists ");

    assertThat(list).hasSize(2);


    LoggedSql.start();
    list = Ebean.find(Customer.class).where()
        .isNotNull("orders.details.product.name")
        .le("id", 4) // ignore others than basicData
        .findList();

    assertThat(LoggedSql.collect().get(0))
      .doesNotContain(" left join ");
    assertThat(list).hasSize(2);
    LoggedSql.stop();
  }

  @Test
  public void testOuterOnFetch() throws Exception {
    LoggedSql.start();

    List<Order> orders1 =  Ebean.find(Order.class).findList();

    assertThat(LoggedSql.collect().get(0))
      .contains(" join o_customer") // ensure that we do not left join the customer
      .doesNotContain(" left join o_customer");


    // now fetch "details" bean, which may be optional
    LoggedSql.start();

    List<Order> orders2 =  Ebean.find(Order.class)
        .fetch("details", "id").findList();

    assertThat(LoggedSql.collect().get(0))
      .contains(" left join o_order_detail ");

    LoggedSql.stop();

    // Note: You would expect, that the two lists are equal.
    // but there is a @Where(clause = "${ta}.id > 0") on the details
    // property, that will filter orders, where details is zero.

    // The produced SQL is:
    // select *  from o_order t0
    //   join o_customer t2 on t2.id = t0.kcustomer_id
    //   left join o_order_detail t1 on t1.order_id = t0.id
    //   where t1.id > 0
    //   order by t0.id, t1.id asc, t1.order_qty asc, t1.cretime desc; --bind()
    //
    // I think this is NOT correct as it may exclude orders without details or with negative ID
    //
    // Correct way could/would be, to add that extra where to the join
    //
    //

    // assertThat(orders2).isEqualTo(orders1);
  }

}
