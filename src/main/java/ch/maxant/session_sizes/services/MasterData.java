package ch.maxant.session_sizes.services;

import static ch.maxant.session_sizes.services.DataService.TEN_MEGS;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

@Repository
@Scope("application")
public class MasterData {

	/**
	 * some master data that only exists in the application once - the scope of
	 * this service is application!
	 */
	private final byte[] masterData = new byte[10 * TEN_MEGS];

	public byte[] getMasterData() {
		return masterData;
	}
}
