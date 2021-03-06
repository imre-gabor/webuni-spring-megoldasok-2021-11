package hu.webuni.hr.minta.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hu.webuni.hr.minta.model.Company;
import hu.webuni.hr.minta.model.Employee;
import hu.webuni.hr.minta.repository.CompanyRepository;
import hu.webuni.hr.minta.repository.EmployeeRepository;

@Service
public class CompanyService {
	
	@Autowired
	private CompanyRepository companyRepository;
	
	@Autowired
	private EmployeeRepository employeeRepository;
	
	@Autowired
	private EmployeeService employeeService;

	@Transactional
	public Company save(Company company) {
		return companyRepository.save(company);
	}

	@Transactional
	public Company update(Company company) {
		if(!companyRepository.existsById(company.getId()))
			return null;
		return companyRepository.save(company);
	}

	public List<Company> findAll() {
		return companyRepository.findAll();
	}

	public Optional<Company> findById(long id) {
		return companyRepository.findById(id);
	}

	@Transactional
	public void delete(long id) {
		companyRepository.deleteById(id);
	}

	@Transactional
	public Company addEmployee(long id, Employee employee) {
		Company company = companyRepository.findByIdWithEmployees(id).get();
		employeeService.setPosition(employee);
		employee = employeeRepository.save(employee);
		company.addEmployee(employee);
		return company;
	}
	
	@Transactional
	public Company deleteEmployee(long id, long employeeId) {
		Company company = companyRepository.findById(id).get();
		Employee employee = employeeRepository.findById(employeeId).get();
		employee.setCompany(null);
		company.getEmployees().remove(employee);
//		employeeRepository.save(employee); --> nem kell a Transactional miatt
		return company;
	}
	
	@Transactional
	public Company replaceEmployees(long id, List<Employee> employees) {
		Company company = companyRepository.findById(id).get();
		company.getEmployees().forEach(e -> e.setCompany(null));
		company.getEmployees().clear();
		
		for(Employee emp: employees) {
			employeeService.setPosition(emp);
			company.addEmployee(employeeRepository.save(emp));
		}
		
		return company;
	}
	
}
