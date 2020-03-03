package io.semla.datasource;

import io.semla.JdbiTest;
import io.semla.cucumber.steps.EntitySteps;
import io.semla.model.User;
import org.junit.Test;


public class JdbiDatasourceTest {

    static {
        JdbiTest.init();
    }

    @Test
    public void we_can_access_the_raw_jdbi() {
        SqlDatasource<User> userDatasource = EntitySteps.datasourceOf(User.class);
        userDatasource.raw().withHandle(handle ->
            handle.createQuery("SELECT COUNT(*) FROM " + userDatasource.ddl().escape(userDatasource.ddl().tablename()))
                .mapTo(Long.class)
                .findOne());
    }

}
