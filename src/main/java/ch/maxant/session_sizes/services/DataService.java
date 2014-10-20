package ch.maxant.session_sizes.services;

import static org.apache.commons.io.FileUtils.ONE_MB;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** a singleton scoped service */
@Service
public class DataService {

    private static final Logger LOGGER = LoggerFactory
	    .getLogger(DataService.class);

    public static final int ONE_MEG = (int) ONE_MB;
    public static final int TEN_MEGS = 10 * ONE_MEG;

    /**
     * this is one problem! any session scoped UI bean referencing the
     * dataservice will also reference the master data, making the measuring of
     * the session size non-trivial.
     */
    @Inject
    private MasterData masterData;

    public byte[] generateData() {

	LOGGER.info(
		"calling generate Data, and using master data which is {} bytes long",
		masterData.getMasterData().length);

	byte[] data = new byte[((int) ONE_MB)];
	System.arraycopy(masterData.getMasterData(), 0, data, 0,
		data.length - 1);
	return data;
    }
}
