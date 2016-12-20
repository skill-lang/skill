package de.ust.skill.generator.haskell;

import java.util.List;
import de.ust.skill.ir.Field;
import de.ust.skill.ir.UserType;
import de.ust.skill.generator.haskell.CodeGenerator;

public class Writer_Access {
	public static void go(List<UserType> IR) {
		for (UserType userType : IR) {
			StringBuilder output = new StringBuilder();
			String uTN = userType.getName().capital();
			String uTL = uTN.toLowerCase();
			String fgs = "get" + uTN + 's';
			String frs = "read" + uTN + 's';
			String fg = "get" + uTN;
			String fr = "read" + uTN;
			String fs = "set" + uTN;
			String fm = "make" + uTN;
			String fd = "delete" + uTN;
			String moduleName = "Access" + CodeGenerator.escape(uTN);
			List<Field> fields = CodeGenerator.getAllFields(userType);

			// GETTERS
			output
			.append("module ").append(moduleName).append(" where\n")
			.append("\nimport qualified Data.Map as M")
			.append("\nimport Types")
			.append("\nimport Deserialize")
			.append("\nimport Controls")
			.append("\nimport Memory")
			.append("\nimport Methods")
			.append("\nimport Follow")
			.append("\nimport Data.Int")
			.append("\nimport Data.IORef")
			.append("\nimport System.IO.Unsafe")
			.append("\n")
			.append("\n").append(fgs).append(" :: State -> [").append(uTN).append(']')
			.append("\n").append(fgs).append(" = go . getTypeDescriptors")
			.append("\n where go [] = []")
			.append("\n       go ((TD id name count fDs subTypes _) : rest)")
			.append("\n            | name == \"").append(uTL).append("\" && (not . null) fDs = go' (0, count) fDs")
			.append("\n            | not $ null (go subTypes) = go subTypes")
			.append("\n            | otherwise = go rest")
			.append("\n               where go' :: (Int, Int) -> [FD] -> [").append(uTN).append("]")
			.append("\n                     go' _ [] = []")
			.append("\n                     go' (id, size) fDs")
			.append("\n                         | id == size = []")
			.append("\n                         | otherwise  = (id `extract").append(uTN).append("` fDs) : go' (id + 1, size) fDs")
			.append("\n")			
			.append("\n").append(frs).append(" :: Int -> IO [").append(uTN).append("]")
			.append("\n").append(frs).append(" index = ").append(fgs).append(" `fmap` readState index")
			.append("\n")
			.append("\n").append(frs).append("' :: IO [").append(uTN).append("]")
			.append("\n").append(frs).append("' = ").append(fgs).append(" `fmap` readState'")
			.append("\n")
			.append("\n").append(fgs).append("'' :: FilePath -> [").append(uTN).append(']')
			.append("\n").append(fgs).append("'' filePath = ").append(fgs).append(" $ (unsafePerformIO . deserialize_) filePath")
			.append("\n")
			.append("\n").append(fg).append(" :: State -> Int -> ").append(uTN)
			.append("\n").append(fg).append(" state index = assure $ go (getTypeDescriptors state) index")
			.append("\n where go :: [TD] -> Int -> Maybe ").append(uTN)
			.append("\n       go [] _ = Nothing")
			.append("\n       go ((TD id name count fDs subTypes _) : rest) index")
			.append("\n            | name == \"").append(uTL).append("\" && (not . null) fDs = Just $ index `extract" + uTN + "` fDs")
			.append("\n            | isJust (go subTypes index) = go subTypes index")
			.append("\n            | otherwise = go rest index")
            .append("\n")
			.append("\n").append(fg).append("' :: Int -> ").append(uTN)
			.append("\n").append(fg).append("' = ").append(fg).append(" $ getState 0")
			.append("\n")
			.append("\n").append(fg).append("'' :: FilePath -> Int -> ").append(uTN)
			.append("\n").append(fg).append("'' filePath index  = unsafePerformIO $ ").append(fr).append("'' filePath index") 
			.append("\n")
			.append("\n").append(fr).append("' :: Int -> IO ").append(uTN)
			.append("\n").append(fr).append("' index = (\\state -> (").append(fg).append(" state index)) `fmap` readState'")
			.append("\n")			
			.append("\n").append(fr).append("'' :: FilePath -> Int -> IO ").append(uTN)
			.append("\n").append(fr).append("'' filePath index = deserialize filePath >> ").append(fr).append("' index")
			.append("\n");

			StringBuilder params = new StringBuilder("(");

			for (int i = 0; i < fields.size(); i++) {
				params.append('f').append(i);

				if (i < fields.size() - 1) {
					params.append(", ");
				}
			}

			params.append(')');

			for (int i = 0; i < fields.size(); i++) {
				Field field = fields.get(i);

				String fN = field.getName().capital();
				String fT = CodeGenerator.getHaskellType(field, CodeGenerator.FOLLOW_REFERENCES);
				String f2 = "get" + uTN + '_' + fN; 
				String f3 = f2 + 's';


				String followPrefix = "";
				String followPostfix = "";

				if (CodeGenerator.isReference(field)) {
					followPrefix = "follow" + fN + " (getTypeDescriptors $ getState 0)";

					UserType type = (UserType) field.getType();
					if(!type.getSubTypes().isEmpty()) {
						followPostfix = "'";
					}
				}
				output
				.append("\n").append(f2).append(" :: ").append(uTN).append(" -> ").append(fT).append(followPostfix)
				.append("\n").append(f2).append(' ').append(params).append(" = ").append(followPrefix).append(" f").append(i)
				.append("\n")
				.append("\n").append(f3).append(" :: State -> [").append(fT).append(followPostfix).append(']')
				.append("\n").append(f3).append(" state = map ").append(f2).append(" (").append(fgs).append(" state)")
				.append("\n")
				.append("\n").append(f3).append("'' :: FilePath -> [").append(fT).append(followPostfix).append(']')
				.append("\n").append(f3).append("'' filePath = ").append(f3).append(" $ (unsafePerformIO . deserialize_) filePath")
				.append("\n");
			}
			
			StringBuilder makeMethodFirstHalf = new StringBuilder();
			StringBuilder deleteMethodFirstHalf = new StringBuilder();
			
			output
			.append("\n").append(fs).append(" :: Int -> Int -> ").append(uTN).append(" -> IO ()")
			.append("\n").append(fs).append(" sI i obj = readState sI >>= \\(s, uTs, tDs) -> writeState sI (s, uTs, assure (go tDs i obj))")
			.append("\n  where go :: [TD] -> Int -> ").append(uTN).append(" -> Maybe [TD]")
			.append("\n        go [] _ _ = Nothing")
			.append("\n        go (tD : r_tDs) i i_c")
			.append("\n            | name == \"").append(uTL).append("\" = Just (TD e1 name e3 (go' fDs i i_c) subtypes e6 : r_tDs)")
			.append("\n            | otherwise = case go subtypes i i_c                                         -- call downward")
			.append("\n                of (Just tDs') -> Just (TD e1 name e3 fDs tDs' e6 : r_tDs)")
			.append("\n                   Nothing -> case go r_tDs i i_c of (Just tDs') -> Just (tD : tDs')  -- call forward")
			.append("\n                                                     Nothing     -> Nothing")
			.append("\n                   where TD e1 name e3 fDs subtypes e6 = tD")
			.append("\n");
			
			makeMethodFirstHalf
			.append("\n").append(fm).append(" :: Int -> ").append(uTN).append(" -> IO ()")
			.append("\n").append(fm).append(" sI obj = readState sI >>= \\(s, uTs, tDs) -> writeState sI (s, uTs, assure (go tDs obj))")
			.append("\n  where go :: [TD] -> ").append(uTN).append(" -> Maybe [TD]")
			.append("\n        go [] _ = Nothing")
			.append("\n        go (tD : r_tDs) i_c")
			.append("\n            | name == \"").append(uTL).append("\" = Just (TD e1 name (count + 1) (go' fDs i_c) subtypes e6 : r_tDs)")
			.append("\n            | otherwise = case go subtypes i_c                                         -- call downward")
			.append("\n                of (Just tDs') -> Just (TD e1 name count fDs tDs' e6 : r_tDs)")
			.append("\n                   Nothing -> case go r_tDs i_c of (Just tDs') -> Just (tD : tDs')  -- call forward")
			.append("\n                                                   Nothing     -> Nothing")
			.append("\n                   where TD e1 name count fDs subtypes e6 = tD")
			.append("\n");
			
			deleteMethodFirstHalf
			.append("\n").append(fd).append(" :: Int -> Int -> IO ()")
			.append("\n").append(fd).append(" sI i = readState sI >>= \\(s, uTs, tDs) -> writeState sI (s, uTs, assure (go tDs i))")
			.append("\n  where go :: [TD] -> Int -> Maybe [TD]")
			.append("\n        go [] _ = Nothing")
			.append("\n        go (tD : r_tDs) i")
			.append("\n            | name == \"").append(uTL).append("\" = Just (TD e1 name (count - 1) (go' fDs i) subtypes e6 : r_tDs)")
			.append("\n            | otherwise = case go subtypes i                                         -- call downward")
			.append("\n                of (Just tDs') -> Just (TD e1 name count fDs tDs' e6 : r_tDs)")
			.append("\n                   Nothing -> case go r_tDs i of (Just tDs') -> Just (tD : tDs')  -- call forward")
			.append("\n                                                 Nothing     -> Nothing")
			.append("\n                   where TD e1 name count fDs subtypes e6 = tD")
			.append("\n");

			StringBuilder fieldDescSignature = new StringBuilder("[");
			StringBuilder objectSignature = new StringBuilder("(");
			StringBuilder returnStatement = new StringBuilder("[");
			StringBuilder where1_pre = new StringBuilder("(");
			StringBuilder where1_post_setter = new StringBuilder("(");
			StringBuilder where1_post_maker = new StringBuilder("(");
			StringBuilder where1_post_deleter = new StringBuilder("(");
			StringBuilder where2_pre = new StringBuilder("(");
			StringBuilder where2_post = new StringBuilder("(");

			for (int i = 0; i < fields.size(); i++) {
				String dataConstructor = CodeGenerator.somethingDataConstructor(userType, fields.get(i));
				
				fieldDescSignature.append("(n" + i + ", f" + i + ", (d" + i + ", t" + i + ", r" + i + "))");
				objectSignature.append("o" + i);
				returnStatement.append("(n" + i + ", g" + i + ", (d" + i + ", t" + i + ", r" + i + "))");
				where1_pre.append("g" + i);
				where1_post_setter.append("replace' i x" + i + " f" + i);
				where1_post_deleter.append("deleteAt i f" + i);
				where1_post_maker.append("f" + i + " ++ [ " + dataConstructor + " o" + i + "]");
				where2_pre.append(" x" + i);
				where2_post.append(dataConstructor).append(" o" + i);

				if (i < fields.size() - 1) {
					fieldDescSignature.append(", ");
					objectSignature.append(", ");
					returnStatement.append(", ");
					where1_pre.append(", ");
					where1_post_setter.append(", ");
					where1_post_maker.append(", ");
					where1_post_deleter.append(", ");
					where2_pre.append(", ");
					where2_post.append(", ");
				}
			}

			fieldDescSignature.append("]");
			objectSignature.append(")");
			returnStatement.append("]");
			where1_pre.append(")");
			where1_post_setter.append(")");
			where1_post_maker.append(")");
			where1_post_deleter.append(")");
			where2_pre.append(")");
			where2_post.append(")");

			output
			.append("\n        go' :: [FD] -> Int -> ").append(uTN).append(" -> [FD]")
			.append("\n        go' ").append(fieldDescSignature).append(" i ").append(objectSignature)
			.append("\n          = ").append(returnStatement)
			.append("\n                where ").append(where1_pre).append(" = ").append(where1_post_setter)
			.append("\n                      ").append(where2_pre).append(" = ").append(where2_post)
			.append("\n")
			.append("\n").append(fs).append("' :: Int -> ").append(uTN).append(" -> IO ()")
			.append("\n").append(fs).append("' = ").append(fs).append(" 0")
			.append("\n")
			.append(makeMethodFirstHalf)
			.append("\n        go' :: [FD] -> ").append(uTN).append(" -> [FD]")
			.append("\n        go' ").append(fieldDescSignature).append(' ').append(objectSignature)
			.append("\n          = ").append(returnStatement)
			.append("\n                where ").append(where1_pre).append(" = ").append(where1_post_maker)
			.append("\n")
			.append("\n").append(fm).append("' :: ").append(uTN).append(" -> IO ()")
			.append("\n").append(fm).append("' = ").append(fm).append(" 0")
			.append("\n")
			.append(deleteMethodFirstHalf)
			.append("\n        go' :: [FD] -> Int -> [FD]")
			.append("\n        go' ").append(fieldDescSignature).append(' ').append("i")
			.append("\n          = ").append(returnStatement)
			.append("\n                where ").append(where1_pre).append(" = ").append(where1_post_deleter)
			.append("\n")
			.append("\n").append(fd).append("' :: Int -> IO ()")
			.append("\n").append(fd).append("' = ").append(fd).append(" 0")
			.append("\n");			

			CodeGenerator.writeInFile(output, "Access" + uTN + ".hs");
		}
	}
}