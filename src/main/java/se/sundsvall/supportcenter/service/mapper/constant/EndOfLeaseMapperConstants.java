package se.sundsvall.supportcenter.service.mapper.constant;

import java.util.Set;

public final class EndOfLeaseMapperConstants {

	/**
	 * Computer names that should not get any message via SysMan. Matched against the letters an asset tag starts with,
	 * taken as a
	 * whole rather than as something a tag begins with, so that PB and MPB stay apart and a tag such as SPARE1 is not
	 * taken for an SP.
	 */
	public static final Set<String> EXCLUDED_ASSET_TAG_PREFIXES = Set.of("EB", "LB", "PB", "PS", "MPB", "MPS", "SP", "CB", "PUB");

	private EndOfLeaseMapperConstants() {}
}
