package org.broadleafcommerce.profile.service;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadleafcommerce.profile.dao.CustomerDao;
import org.broadleafcommerce.profile.domain.Customer;
import org.broadleafcommerce.profile.util.EntityConfiguration;
import org.broadleafcommerce.profile.util.PasswordChange;
import org.springframework.security.Authentication;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.providers.encoding.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service("customerService")
public class CustomerServiceImpl implements CustomerService {

    /** Logger for this class and subclasses */
    protected final Log logger = LogFactory.getLog(getClass());

    @Resource
    private CustomerDao customerDao;

    @Resource
    private IdGenerationService idGenerationService;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private EntityConfiguration entityConfiguration;

    // @Resource(name = "saltSource")
    // private SaltSource saltSource;

    public Customer saveCustomer(Customer customer) {
        if (!customer.isRegistered()) {
            customer.setRegistered(true);
        }
        if (customer.getUnencodedPassword() != null) {
            customer.setPassword(passwordEncoder.encodePassword(customer.getUnencodedPassword(), null));
        }

        // let's make sure they entered a new challenge answer (we will populate the password field with hashed values so check that they have changed id
        if (customer.getUnencodedChallengeAnswer() != null && !customer.getUnencodedChallengeAnswer().equals(customer.getChallengeAnswer())) {
            customer.setChallengeAnswer(passwordEncoder.encodePassword(customer.getUnencodedChallengeAnswer(), null));
        }
        return customerDao.maintainCustomer(customer);
    }

    public Customer registerCustomer(Customer customer) {
        customer.setRegistered(true);
        return saveCustomer(customer);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Customer readCustomerByEmail(String emailAddress) {
        return customerDao.readCustomerByEmail(emailAddress);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Customer changePassword(PasswordChange passwordChange) {
        Customer customer = readCustomerByUsername(passwordChange.getUsername());
        customer.setUnencodedPassword(passwordChange.getNewPassword());
        customer.setPasswordChangeRequired(passwordChange.getPasswordChangeRequired());
        customer = saveCustomer(customer);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(passwordChange.getUsername(), passwordChange.getNewPassword(), auth.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authRequest);
        auth.setAuthenticated(false);
        return customer;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Customer readCustomerByUsername(String username) {
        return customerDao.readCustomerByUsername(username);
    }

    @Override
    public Customer readCustomerById(Long id) {
        return customerDao.readCustomerById(id);
    }

    public void setCustomerDao(CustomerDao customerDao) {
        this.customerDao = customerDao;
    }

    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public Customer createCustomerFromId(Long customerId) {
        Customer customer = customerId != null ? readCustomerById(customerId) : null;
        if (customer == null) {
            customer = (Customer) entityConfiguration.createEntityInstance("org.broadleafcommerce.profile.domain.Customer");
            customer.setId(idGenerationService.findNextId("org.broadleafcommerce.profile.domain.Customer"));
        }
        return customer;
    }
}
