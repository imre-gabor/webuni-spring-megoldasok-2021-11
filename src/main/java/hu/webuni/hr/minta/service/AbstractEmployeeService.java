package hu.webuni.hr.minta.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hu.webuni.hr.minta.model.Employee;
import hu.webuni.hr.minta.model.Position;
import hu.webuni.hr.minta.repository.EmployeeRepository;
import hu.webuni.hr.minta.repository.PositionRepository;

@Service
public abstract class AbstractEmployeeService implements EmployeeService {
	
	@Autowired
	private EmployeeRepository employeeRepository;

	@Autowired
	private PositionRepository positionRepository;

	@Override
	@Transactional
	public Employee save(Employee employee) {
		clearCompanyAndSetPosition(employee);
		return employeeRepository.save(employee);
	}
	
	private void clearCompanyAndSetPosition(Employee employee) {
		employee.setCompany(null);
		Position position = null;
		String positionName = employee.getPosition().getName();
		if(positionName != null) {
			List<Position> positions = positionRepository.findByName(positionName);
			if(positions.isEmpty()) {
				position = positionRepository.save(new Position(positionName, null));
			} else {
				position = positions.get(0);
			}
		}
		employee.setPosition(position);
	}


	@Override
	public Employee update(Employee employee) {
		if(!employeeRepository.existsById(employee.getEmployeeId()))
			return null;
		
		clearCompanyAndSetPosition(employee);
		return employeeRepository.save(employee);
	}

	@Override
	public List<Employee> findAll() {
		return employeeRepository.findAll();
	}

	@Override
	public Optional<Employee> findById(long id) {
		return employeeRepository.findById(id);
	}

	@Override
	public void delete(long id) {
		employeeRepository.deleteById(id);
	}
	
}
