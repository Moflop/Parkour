package mod.arcomit.parkour.core.client.animation.player;

import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.animation.RawAnimation;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import com.zigythebird.playeranimcore.enums.PlayState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParkourAnimationControllerTest {
	private ParkourAnimationController controller() {
		return new ParkourAnimationController(null, (controller, data, setter) -> PlayState.CONTINUE);
	}

	private void frame(ParkourAnimationController controller) {
		var data = new AnimationData(0, 0);
		controller.tick(data);
		controller.setupAnim(data);
	}

	@Test
	void shiftFadeStopsOnceAndReturnsTheFirstPersonRenderer() {
		var controller = controller();
		controller.playAnimation(RawAnimation.begin().thenWait(30), ParkourAnimType.STATE, 1, List.of());
		controller.setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL);
		frame(controller);
		controller.stopAnimation(5);
		frame(controller);
		controller.stopAnimation(5);
		for (int i = 0; i < 20; i++) frame(controller);
		assertFalse(controller.isActive());
		assertNull(controller.getCurrentType());
		assertEquals(FirstPersonMode.NONE, controller.getFirstPersonMode());
		assertTrue(controller.getModifiers().isEmpty());
	}

	@Test
	void passiveDefaultSyncDoesNotClearAnActionOrItsFirstPersonMode() {
		var controller = controller();
		assertTrue(controller.playFadeOutAnim(RawAnimation.begin().thenWait(11), ParkourAnimType.ACTION, 1, 2, List.of()));
		controller.setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL);
		for (int i = 0; i < 8; i++) {
			frame(controller);
			controller.stopAnimation(5);
			assertTrue(controller.isActive());
			assertEquals(FirstPersonMode.THIRD_PERSON_MODEL, controller.getFirstPersonMode());
		}
		for (int i = 0; i < 8; i++) frame(controller);
		assertFalse(controller.isActive());
		assertEquals(FirstPersonMode.NONE, controller.getFirstPersonMode());
		assertTrue(controller.getModifiers().isEmpty());
	}

	@Test
	void rejectedActionDoesNotAttachAFadeToTheActiveState() {
		var controller = controller();
		controller.playAnimation(RawAnimation.begin().thenWait(30), ParkourAnimType.STATE, 1, List.of());
		int modifiers = controller.getModifiers().size();
		assertFalse(controller.playFadeOutAnim(RawAnimation.begin().thenWait(11), ParkourAnimType.ACTION, 1, 2, List.of()));
		assertEquals(modifiers, controller.getModifiers().size());
	}
}
